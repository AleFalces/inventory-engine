package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.config.JwtUtil;
import com.omnicore.inventory_engine.domain.entity.RefreshToken;
import com.omnicore.inventory_engine.domain.entity.Tenant;
import com.omnicore.inventory_engine.domain.repository.RefreshTokenRepository;
import com.omnicore.inventory_engine.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import com.omnicore.inventory_engine.domain.entity.TenantRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldReturnTokenPairWhenCredentialsAreValid() {
        var tenant = Tenant.builder()
                .tenantId("acme")
                .passwordHash("$2a$hashed")
                .build();

        when(tenantRepository.findByTenantId("acme")).thenReturn(Optional.of(tenant));
        when(passwordEncoder.matches("secret", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(eq("acme"), any())).thenReturn("access.token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = authService.login("acme", "secret");

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isNotBlank();
    }

    // ─── refresh() ────────────────────────────────────────────────────────────

    @Test
    void shouldReturnNewTokenPairWhenRefreshTokenIsValid() {
        var stored = RefreshToken.builder()
                .token("valid-uuid")
                .tenantId("acme")
                .role(TenantRole.ADMIN)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-uuid")).thenReturn(Optional.of(stored));
        when(jwtUtil.generateAccessToken("acme", TenantRole.ADMIN)).thenReturn("new.access.token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = authService.refresh("valid-uuid");

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void shouldThrowWhenRefreshTokenNotFound() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldThrowWhenRefreshTokenIsRevoked() {
        var stored = RefreshToken.builder()
                .token("revoked-uuid")
                .tenantId("acme")
                .role(TenantRole.VIEWER)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken("revoked-uuid")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("revoked-uuid"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldThrowWhenRefreshTokenIsExpired() {
        var stored = RefreshToken.builder()
                .token("expired-uuid")
                .tenantId("acme")
                .role(TenantRole.VIEWER)
                .expiresAt(Instant.now().minusSeconds(1))  // ya expiró
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-uuid")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("expired-uuid"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldThrowWhenTenantDoesNotExist() {
        when(tenantRepository.findByTenantId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ─── register() ───────────────────────────────────────────────────────────

    @Test
    void shouldRegisterNewTenant() {
        when(tenantRepository.existsByTenantId("acme")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$hashed");
        when(tenantRepository.save(any())).thenReturn(
                Tenant.builder().tenantId("acme").passwordHash("$2a$hashed").build());

        var response = authService.register("acme", "secret", TenantRole.VIEWER);

        assertThat(response.tenantId()).isEqualTo("acme");
    }

    @Test
    void shouldThrowWhenTenantAlreadyExists() {
        when(tenantRepository.existsByTenantId("acme")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("acme", "secret", TenantRole.VIEWER))
                .isInstanceOf(TenantAlreadyExistsException.class);
    }

    @Test
    void shouldThrowWhenPasswordIsWrong() {
        var tenant = Tenant.builder()
                .tenantId("acme")
                .passwordHash("$2a$hashed")
                .build();

        when(tenantRepository.findByTenantId("acme")).thenReturn(Optional.of(tenant));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("acme", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

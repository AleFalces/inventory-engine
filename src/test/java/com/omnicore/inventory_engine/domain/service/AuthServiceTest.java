package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.config.JwtUtil;
import com.omnicore.inventory_engine.domain.entity.Tenant;
import com.omnicore.inventory_engine.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() {
        var tenant = Tenant.builder()
                .tenantId("acme")
                .passwordHash("$2a$hashed")
                .build();

        when(tenantRepository.findByTenantId("acme")).thenReturn(Optional.of(tenant));
        when(passwordEncoder.matches("secret", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("acme")).thenReturn("jwt.token.here");

        var response = authService.login("acme", "secret");

        assertThat(response.token()).isEqualTo("jwt.token.here");
    }

    @Test
    void shouldThrowWhenTenantDoesNotExist() {
        when(tenantRepository.findByTenantId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
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

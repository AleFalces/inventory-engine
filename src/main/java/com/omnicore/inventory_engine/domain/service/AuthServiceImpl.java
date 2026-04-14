package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.api.dto.RegisterResponse;
import com.omnicore.inventory_engine.config.JwtUtil;
import com.omnicore.inventory_engine.domain.entity.RefreshToken;
import com.omnicore.inventory_engine.domain.entity.Tenant;
import com.omnicore.inventory_engine.domain.entity.TenantRole;
import com.omnicore.inventory_engine.domain.repository.RefreshTokenRepository;
import com.omnicore.inventory_engine.domain.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7 * 24 * 3600L; // 7 días

    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(TenantRepository tenantRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.tenantRepository = tenantRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public LoginResponse login(String tenantId, String password) {
        var tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, tenant.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildTokenPair(tenant.getTenantId(), tenant.getRole());
    }

    @Override
    @Transactional
    public LoginResponse refresh(String refreshTokenValue) {
        var stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        // Rotar: revocar el token actual
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildTokenPair(stored.getTenantId(), stored.getRole());
    }

    @Override
    @Transactional
    public RegisterResponse register(String tenantId, String password, TenantRole role) {
        if (tenantRepository.existsByTenantId(tenantId)) {
            throw new TenantAlreadyExistsException(tenantId);
        }
        var tenant = Tenant.builder()
                .tenantId(tenantId)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();
        tenantRepository.save(tenant);
        return new RegisterResponse(tenantId);
    }

    private LoginResponse buildTokenPair(String tenantId, TenantRole role) {
        String accessToken = jwtUtil.generateAccessToken(tenantId, role);
        String refreshTokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshTokenValue)
                .tenantId(tenantId)
                .role(role)
                .expiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS))
                .build());

        return new LoginResponse(accessToken, refreshTokenValue);
    }
}

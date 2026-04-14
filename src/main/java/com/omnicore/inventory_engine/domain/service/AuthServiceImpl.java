package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.config.JwtUtil;
import com.omnicore.inventory_engine.domain.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(TenantRepository tenantRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(String tenantId, String password) {
        var tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, tenant.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(jwtUtil.generateToken(tenantId));
    }
}

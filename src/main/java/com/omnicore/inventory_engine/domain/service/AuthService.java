package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.api.dto.RegisterResponse;
import com.omnicore.inventory_engine.domain.entity.TenantRole;

public interface AuthService {
    LoginResponse login(String tenantId, String password);
    RegisterResponse register(String tenantId, String password, TenantRole role);
    LoginResponse refresh(String refreshToken);
}

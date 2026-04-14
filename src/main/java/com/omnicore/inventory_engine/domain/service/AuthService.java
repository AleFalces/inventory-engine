package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.api.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(String tenantId, String password);
}

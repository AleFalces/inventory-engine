package com.omnicore.inventory_engine.api.controller;

import com.omnicore.inventory_engine.api.dto.LoginRequest;
import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.domain.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.tenantId(), request.password());
    }
}

package com.omnicore.inventory_engine.api.controller;

import com.omnicore.inventory_engine.api.dto.LoginRequest;
import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.api.dto.RefreshRequest;
import com.omnicore.inventory_engine.api.dto.RegisterRequest;
import com.omnicore.inventory_engine.api.dto.RegisterResponse;
import com.omnicore.inventory_engine.domain.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.tenantId(), request.password());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.tenantId(), request.password(), request.role());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }
}

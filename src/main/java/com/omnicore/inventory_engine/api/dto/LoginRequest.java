package com.omnicore.inventory_engine.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String tenantId,
        @NotBlank String password
) {}

package com.omnicore.inventory_engine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String tenantId,
        @NotBlank @Size(min = 8) String password
) {}

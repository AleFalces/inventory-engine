package com.omnicore.inventory_engine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero Integer stock
) {}

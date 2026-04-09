package com.omnicore.inventory_engine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull Integer delta,
        @NotBlank String reason
) {}

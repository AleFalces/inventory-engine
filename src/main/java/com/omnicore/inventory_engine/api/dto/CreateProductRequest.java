package com.omnicore.inventory_engine.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(

        @NotBlank
        String sku,

        @NotBlank
        String name,

        String description,

        @NotNull
        @Min(0)
        Integer stock
) {}

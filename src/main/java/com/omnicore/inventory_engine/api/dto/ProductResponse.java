package com.omnicore.inventory_engine.api.dto;

public record ProductResponse(
        Long id,
        String tenantId,
        String sku,
        String name,
        String description,
        Integer stock
) {}

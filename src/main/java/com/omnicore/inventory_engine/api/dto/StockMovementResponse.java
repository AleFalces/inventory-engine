package com.omnicore.inventory_engine.api.dto;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        String tenantId,
        String productSku,
        Integer delta,
        String reason,
        Integer stockBefore,
        Integer stockAfter,
        Instant createdAt
) {}

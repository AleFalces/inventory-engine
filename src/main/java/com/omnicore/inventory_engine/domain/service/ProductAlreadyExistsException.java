package com.omnicore.inventory_engine.domain.service;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException(String tenantId, String sku) {
        super("Product already exists: sku=%s tenant=%s".formatted(sku, tenantId));
    }
}

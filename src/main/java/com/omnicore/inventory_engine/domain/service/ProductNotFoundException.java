package com.omnicore.inventory_engine.domain.service;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String tenantId, String sku) {
        super("Product not found: sku=%s tenant=%s".formatted(sku, tenantId));
    }
}

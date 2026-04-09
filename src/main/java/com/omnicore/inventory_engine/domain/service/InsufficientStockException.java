package com.omnicore.inventory_engine.domain.service;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String tenantId, String sku, int available, int requested) {
        super("Insufficient stock for SKU '" + sku + "' in tenant '" + tenantId +
              "': available=" + available + ", requested=" + Math.abs(requested));
    }
}

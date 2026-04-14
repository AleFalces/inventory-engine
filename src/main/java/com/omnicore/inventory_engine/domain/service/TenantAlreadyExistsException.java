package com.omnicore.inventory_engine.domain.service;

public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String tenantId) {
        super("Tenant already exists: " + tenantId);
    }
}

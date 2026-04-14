package com.omnicore.inventory_engine.api.dto;

import com.omnicore.inventory_engine.domain.entity.TenantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String tenantId,
        @NotBlank @Size(min = 8) String password,
        TenantRole role
) {
    public RegisterRequest(@NotBlank String tenantId, @NotBlank @Size(min = 8) String password) {
        this(tenantId, password, TenantRole.VIEWER);
    }
}

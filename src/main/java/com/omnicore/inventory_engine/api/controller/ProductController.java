package com.omnicore.inventory_engine.api.controller;

import com.omnicore.inventory_engine.api.dto.CreateProductRequest;
import com.omnicore.inventory_engine.api.dto.ProductResponse;
import com.omnicore.inventory_engine.api.dto.StockAdjustRequest;
import com.omnicore.inventory_engine.api.dto.StockMovementResponse;
import com.omnicore.inventory_engine.api.dto.UpdateProductRequest;
import com.omnicore.inventory_engine.domain.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// TODO Phase 4: reemplazar @RequestHeader("X-Tenant-ID") por extracción desde JWT/SecurityContext
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(
            @AuthenticationPrincipal String tenantId,
            @Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(tenantId, request);
    }

    @GetMapping
    public Page<ProductResponse> findAll(
            @AuthenticationPrincipal String tenantId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.findAllByTenant(tenantId, pageable);
    }

    @GetMapping("/{sku}")
    public ProductResponse findBySku(
            @AuthenticationPrincipal String tenantId,
            @PathVariable String sku) {
        return productService.findByTenantAndSku(tenantId, sku);
    }

    @PutMapping("/{sku}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(
            @AuthenticationPrincipal String tenantId,
            @PathVariable String sku,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(tenantId, sku, request);
    }

    @DeleteMapping("/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @AuthenticationPrincipal String tenantId,
            @PathVariable String sku) {
        productService.deleteProduct(tenantId, sku);
    }

    @GetMapping("/{sku}/movements")
    public List<StockMovementResponse> movements(
            @AuthenticationPrincipal String tenantId,
            @PathVariable String sku) {
        return productService.findMovements(tenantId, sku);
    }

    @PostMapping("/{sku}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse adjust(
            @AuthenticationPrincipal String tenantId,
            @PathVariable String sku,
            @Valid @RequestBody StockAdjustRequest request) {
        return productService.adjustStock(tenantId, sku, request);
    }
}

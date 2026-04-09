package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.api.dto.CreateProductRequest;
import com.omnicore.inventory_engine.api.dto.ProductResponse;
import com.omnicore.inventory_engine.api.dto.StockAdjustRequest;
import com.omnicore.inventory_engine.api.dto.UpdateProductRequest;
import com.omnicore.inventory_engine.api.mapper.ProductMapper;
import com.omnicore.inventory_engine.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(String tenantId, CreateProductRequest request) {
        productRepository.findByTenantIdAndSku(tenantId, request.sku())
                .ifPresent(p -> {
                    throw new ProductAlreadyExistsException(tenantId, request.sku());
                });

        var product = productMapper.toEntity(request);
        product.setTenantId(tenantId);

        return productMapper.toResponse(productRepository.save(product));
    }

    public List<ProductResponse> findAllByTenant(String tenantId) {
        return productRepository.findAllByTenantId(tenantId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse findByTenantAndSku(String tenantId, String sku) {
        return productRepository.findByTenantIdAndSku(tenantId, sku)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(tenantId, sku));
    }

    @Transactional
    public ProductResponse updateProduct(String tenantId, String sku, UpdateProductRequest request) {
        var product = productRepository.findByTenantIdAndSku(tenantId, sku)
                .orElseThrow(() -> new ProductNotFoundException(tenantId, sku));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setStock(request.stock());

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(String tenantId, String sku) {
        var product = productRepository.findByTenantIdAndSku(tenantId, sku)
                .orElseThrow(() -> new ProductNotFoundException(tenantId, sku));

        productRepository.delete(product);
    }

    @Transactional
    public ProductResponse adjustStock(String tenantId, String sku, StockAdjustRequest request) {
        var product = productRepository.findByTenantIdAndSku(tenantId, sku)
                .orElseThrow(() -> new ProductNotFoundException(tenantId, sku));

        int newStock = product.getStock() + request.delta();
        if (newStock < 0) {
            throw new InsufficientStockException(tenantId, sku, product.getStock(), request.delta());
        }

        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }
}

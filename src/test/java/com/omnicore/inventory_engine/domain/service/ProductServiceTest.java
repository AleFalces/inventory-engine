package com.omnicore.inventory_engine.domain.service;

import com.omnicore.inventory_engine.api.dto.CreateProductRequest;
import com.omnicore.inventory_engine.api.dto.ProductResponse;
import com.omnicore.inventory_engine.api.mapper.ProductMapper;
import com.omnicore.inventory_engine.domain.entity.Product;
import com.omnicore.inventory_engine.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    // ─── Test 1: Crear producto exitosamente ───────────────────────────────────

    @Test
    void shouldCreateProductSuccessfully() {
        var request = new CreateProductRequest("SKU-001", "Widget Pro", "Desc", 100);
        var entity   = buildProduct("tenant-a", "SKU-001");
        var response = buildResponse("tenant-a", "SKU-001");

        when(productRepository.findByTenantIdAndSku("tenant-a", "SKU-001")).thenReturn(Optional.empty());
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(any(Product.class))).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.createProduct("tenant-a", request);

        assertThat(result.sku()).isEqualTo("SKU-001");
        assertThat(result.tenantId()).isEqualTo("tenant-a");
        verify(productRepository).save(any(Product.class));
    }

    // ─── Test 2: SKU duplicado dentro del mismo tenant lanza excepción ─────────

    @Test
    void shouldThrowExceptionWhenSkuAlreadyExistsForTenant() {
        var request  = new CreateProductRequest("SKU-001", "Widget", null, 50);
        var existing = buildProduct("tenant-a", "SKU-001");

        when(productRepository.findByTenantIdAndSku("tenant-a", "SKU-001"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.createProduct("tenant-a", request))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining("SKU-001");

        verify(productRepository, never()).save(any());
    }

    // ─── Test 3: SKU duplicado en otro tenant NO es problema ──────────────────

    @Test
    void shouldAllowSameSkuForDifferentTenants() {
        var request  = new CreateProductRequest("SKU-001", "Widget", null, 50);
        var entity   = buildProduct("tenant-b", "SKU-001");
        var response = buildResponse("tenant-b", "SKU-001");

        when(productRepository.findByTenantIdAndSku("tenant-b", "SKU-001")).thenReturn(Optional.empty());
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(any())).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.createProduct("tenant-b", request);

        assertThat(result.tenantId()).isEqualTo("tenant-b");
    }

    // ─── Test 4: Listar todos los productos de un tenant ──────────────────────

    @Test
    void shouldReturnAllProductsForTenant() {
        var entity1  = buildProduct("tenant-a", "SKU-001");
        var entity2  = buildProduct("tenant-a", "SKU-002");
        var response1 = buildResponse("tenant-a", "SKU-001");
        var response2 = buildResponse("tenant-a", "SKU-002");

        when(productRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(entity1, entity2));
        when(productMapper.toResponse(entity1)).thenReturn(response1);
        when(productMapper.toResponse(entity2)).thenReturn(response2);

        List<ProductResponse> result = productService.findAllByTenant("tenant-a");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponse::tenantId).containsOnly("tenant-a");
    }

    // ─── Test 5: Producto no encontrado lanza excepción ───────────────────────

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findByTenantIdAndSku("tenant-a", "SKU-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByTenantAndSku("tenant-a", "SKU-999"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("SKU-999");
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Product buildProduct(String tenantId, String sku) {
        return Product.builder()
                .id(1L).tenantId(tenantId).sku(sku).name("Widget").stock(100).build();
    }

    private ProductResponse buildResponse(String tenantId, String sku) {
        return new ProductResponse(1L, tenantId, sku, "Widget", null, 100);
    }
}

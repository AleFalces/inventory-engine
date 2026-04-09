package com.omnicore.inventory_engine.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.omnicore.inventory_engine.api.dto.CreateProductRequest;
import com.omnicore.inventory_engine.api.dto.ProductResponse;
import com.omnicore.inventory_engine.api.dto.StockAdjustRequest;
import com.omnicore.inventory_engine.api.dto.UpdateProductRequest;
import com.omnicore.inventory_engine.domain.service.InsufficientStockException;
import com.omnicore.inventory_engine.domain.service.ProductAlreadyExistsException;
import com.omnicore.inventory_engine.domain.service.ProductNotFoundException;
import com.omnicore.inventory_engine.domain.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ─── POST /api/v1/products ─────────────────────────────────────────────────

    @Test
    void shouldCreateProductAndReturn201() throws Exception {
        var request  = new CreateProductRequest("SKU-001", "Widget Pro", "Desc", 100);
        var response = new ProductResponse(1L, "tenant-a", "SKU-001", "Widget Pro", "Desc", 100);

        when(productService.createProduct(eq("tenant-a"), any(CreateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"));
    }

    @Test
    void shouldReturn409WhenSkuAlreadyExists() throws Exception {
        var request = new CreateProductRequest("SKU-001", "Widget", null, 10);

        when(productService.createProduct(eq("tenant-a"), any()))
                .thenThrow(new ProductAlreadyExistsException("tenant-a", "SKU-001"));

        mockMvc.perform(post("/api/v1/products")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── GET /api/v1/products ──────────────────────────────────────────────────

    @Test
    void shouldReturnAllProductsForTenant() throws Exception {
        var products = List.of(
                new ProductResponse(1L, "tenant-a", "SKU-001", "Widget", null, 50),
                new ProductResponse(2L, "tenant-a", "SKU-002", "Gadget", null, 30)
        );
        Page<ProductResponse> productPage = new PageImpl<>(products, PageRequest.of(0, 20), products.size());

        when(productService.findAllByTenant(eq("tenant-a"), any(Pageable.class))).thenReturn(productPage);

        mockMvc.perform(get("/api/v1/products")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$.content[1].sku").value("SKU-002"));
    }

    // ─── GET /api/v1/products (paginado) ──────────────────────────────────────

    @Test
    void shouldReturnPagedProductList() throws Exception {
        var products = List.of(
                new ProductResponse(1L, "tenant-a", "SKU-001", "Widget", null, 50),
                new ProductResponse(2L, "tenant-a", "SKU-002", "Gadget", null, 30)
        );
        var pageable    = PageRequest.of(0, 2);
        Page<ProductResponse> productPage = new PageImpl<>(products, pageable, 5);

        when(productService.findAllByTenant(eq("tenant-a"), any(Pageable.class))).thenReturn(productPage);

        mockMvc.perform(get("/api/v1/products")
                        .header("X-Tenant-ID", "tenant-a")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    // ─── GET /api/v1/products/{sku} ────────────────────────────────────────────

    @Test
    void shouldReturnProductBySku() throws Exception {
        var response = new ProductResponse(1L, "tenant-a", "SKU-001", "Widget", null, 50);

        when(productService.findByTenantAndSku("tenant-a", "SKU-001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/SKU-001")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.findByTenantAndSku("tenant-a", "SKU-999"))
                .thenThrow(new ProductNotFoundException("tenant-a", "SKU-999"));

        mockMvc.perform(get("/api/v1/products/SKU-999")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── PUT /api/v1/products/{sku} ────────────────────────────────────────────

    @Test
    void shouldUpdateProductAndReturn200() throws Exception {
        var request  = new UpdateProductRequest("Updated Widget", "New desc", 75);
        var response = new ProductResponse(1L, "tenant-a", "SKU-001", "Updated Widget", "New desc", 75);

        when(productService.updateProduct(eq("tenant-a"), eq("SKU-001"), any(UpdateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/products/SKU-001")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Widget"))
                .andExpect(jsonPath("$.stock").value(75));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentProduct() throws Exception {
        var request = new UpdateProductRequest("Name", null, 10);

        when(productService.updateProduct(eq("tenant-a"), eq("SKU-999"), any()))
                .thenThrow(new ProductNotFoundException("tenant-a", "SKU-999"));

        mockMvc.perform(put("/api/v1/products/SKU-999")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── DELETE /api/v1/products/{sku} ────────────────────────────────────────

    @Test
    void shouldDeleteProductAndReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/SKU-001")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct("tenant-a", "SKU-001");
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentProduct() throws Exception {
        doThrow(new ProductNotFoundException("tenant-a", "SKU-999"))
                .when(productService).deleteProduct("tenant-a", "SKU-999");

        mockMvc.perform(delete("/api/v1/products/SKU-999")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── POST /api/v1/products/{sku}/adjust ───────────────────────────────────

    @Test
    void shouldAdjustStockAndReturn200() throws Exception {
        var request  = new StockAdjustRequest(-10, "OUT");
        var response = new ProductResponse(1L, "tenant-a", "SKU-001", "Widget", null, 90);

        when(productService.adjustStock(eq("tenant-a"), eq("SKU-001"), any(StockAdjustRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/products/SKU-001/adjust")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(90));
    }

    @Test
    void shouldReturn404WhenAdjustingNonExistentProduct() throws Exception {
        var request = new StockAdjustRequest(10, "IN");

        when(productService.adjustStock(eq("tenant-a"), eq("SKU-999"), any()))
                .thenThrow(new ProductNotFoundException("tenant-a", "SKU-999"));

        mockMvc.perform(post("/api/v1/products/SKU-999/adjust")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn422WhenStockWouldGoBelowZero() throws Exception {
        var request = new StockAdjustRequest(-999, "OUT");

        when(productService.adjustStock(eq("tenant-a"), eq("SKU-001"), any()))
                .thenThrow(new InsufficientStockException("tenant-a", "SKU-001", 50, -999));

        mockMvc.perform(post("/api/v1/products/SKU-001/adjust")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists());
    }
}

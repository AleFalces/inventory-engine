package com.omnicore.inventory_engine.api.controller;

import com.omnicore.inventory_engine.api.dto.StockMovementResponse;
import com.omnicore.inventory_engine.domain.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StockMovementControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tenant-a", null, List.of()));
        mockMvc = MockMvcBuilders
                .standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── GET /api/v1/products/{sku}/movements ──────────────────────────────────

    @Test
    void shouldReturnMovementHistoryForProduct() throws Exception {
        var movements = List.of(
                new StockMovementResponse(1L, "tenant-a", "SKU-001", -10, "OUT", 100, 90,  Instant.now()),
                new StockMovementResponse(2L, "tenant-a", "SKU-001",  50, "IN",   90, 140, Instant.now())
        );

        when(productService.findMovements("tenant-a", "SKU-001")).thenReturn(movements);

        mockMvc.perform(get("/api/v1/products/SKU-001/movements")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].delta").value(-10))
                .andExpect(jsonPath("$[1].delta").value(50));
    }
}

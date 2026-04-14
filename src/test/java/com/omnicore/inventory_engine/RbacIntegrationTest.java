package com.omnicore.inventory_engine;

import com.omnicore.inventory_engine.config.JwtUtil;
import com.omnicore.inventory_engine.domain.entity.Tenant;
import com.omnicore.inventory_engine.domain.entity.TenantRole;
import com.omnicore.inventory_engine.domain.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class RbacIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        tenantRepository.deleteAll();
        tenantRepository.save(Tenant.builder()
                .tenantId("admin-tenant")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .role(TenantRole.ADMIN)
                .build());
        tenantRepository.save(Tenant.builder()
                .tenantId("viewer-tenant")
                .passwordHash(passwordEncoder.encode("viewerpass"))
                .role(TenantRole.VIEWER)
                .build());

        adminToken  = jwtUtil.generateToken("admin-tenant",  TenantRole.ADMIN);
        viewerToken = jwtUtil.generateToken("viewer-tenant", TenantRole.VIEWER);
    }

    // ─── VIEWER puede leer ────────────────────────────────────────────────────

    @Test
    void viewerCanReadProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    // ─── VIEWER no puede escribir ─────────────────────────────────────────────

    @Test
    void viewerCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Widget\",\"stock\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotUpdateProduct() throws Exception {
        mockMvc.perform(put("/api/v1/products/SKU-001")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Widget\",\"stock\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/SKU-001")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotAdjustStock() throws Exception {
        mockMvc.perform(post("/api/v1/products/SKU-001/adjust")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":10,\"reason\":\"restock\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── ADMIN puede todo ─────────────────────────────────────────────────────

    @Test
    void adminCanCreateProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Widget\",\"stock\":10}"))
                .andExpect(status().isCreated());
    }
}

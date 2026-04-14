package com.omnicore.inventory_engine;

import com.omnicore.inventory_engine.domain.entity.Tenant;
import com.omnicore.inventory_engine.domain.entity.TenantRole;
import com.omnicore.inventory_engine.domain.repository.TenantRepository;
import tools.jackson.databind.ObjectMapper;
import com.omnicore.inventory_engine.api.dto.LoginRequest;
import com.omnicore.inventory_engine.api.dto.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class LoginIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        tenantRepository.deleteAll();
        tenantRepository.save(Tenant.builder()
                .tenantId("acme")
                .passwordHash(passwordEncoder.encode("secret"))
                .role(TenantRole.ADMIN)
                .build());
    }

    // ─── Test 1: Login exitoso → 200 + token ──────────────────────────────────

    @Test
    void shouldReturnJwtTokenOnValidLogin() throws Exception {
        var request = new LoginRequest("acme", "secret");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ─── Test 2: Token válido permite acceder a endpoint protegido ────────────

    @Test
    void shouldAllowAccessToProtectedEndpointWithValidToken() throws Exception {
        var loginRequest = new LoginRequest("acme", "secret");

        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Widget\",\"stock\":10}"))
                .andExpect(status().isCreated());
    }

    // ─── Test 3: Password incorrecta → 401 ────────────────────────────────────

    @Test
    void shouldReturn401OnWrongPassword() throws Exception {
        var request = new LoginRequest("acme", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── Test 4: Tenant inexistente → 401 ────────────────────────────────────

    @Test
    void shouldReturn401OnUnknownTenant() throws Exception {
        var request = new LoginRequest("unknown", "secret");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── Test 5: register → login → acceso a endpoint protegido ──────────────

    @Test
    void shouldAllowFullFlowRegisterThenLoginThenAccess() throws Exception {
        var registerRequest = new RegisterRequest("newcorp", "mypassword", TenantRole.ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("newcorp"));

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("newcorp", "mypassword"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = new ObjectMapper()
                .readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Widget\",\"stock\":10}"))
                .andExpect(status().isCreated());
    }

    // ─── Test 6: registro duplicado → 409 ────────────────────────────────────

    @Test
    void shouldReturn409WhenRegisteringExistingTenant() throws Exception {
        var request = new RegisterRequest("acme", "supersecret");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }
}

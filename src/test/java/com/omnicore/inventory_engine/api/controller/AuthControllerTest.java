package com.omnicore.inventory_engine.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.omnicore.inventory_engine.api.dto.LoginRequest;
import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.api.dto.RegisterRequest;
import com.omnicore.inventory_engine.api.dto.RegisterResponse;
import com.omnicore.inventory_engine.domain.service.AuthService;
import com.omnicore.inventory_engine.domain.service.InvalidCredentialsException;
import com.omnicore.inventory_engine.domain.service.TenantAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─── POST /api/v1/auth/login ──────────────────────────────────────────────

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
        var request  = new LoginRequest("acme", "secret");
        var response = new LoginResponse("eyJhbGciOiJIUzI1NiJ9.test.token");

        when(authService.login("acme", "secret")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void shouldReturn401WhenPasswordIsWrong() throws Exception {
        var request = new LoginRequest("acme", "wrong-password");

        when(authService.login("acme", "wrong-password"))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn401WhenTenantDoesNotExist() throws Exception {
        var request = new LoginRequest("unknown-tenant", "secret");

        when(authService.login("unknown-tenant", "secret"))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── POST /api/v1/auth/register ───────────────────────────────────────────

    @Test
    void shouldRegisterTenantAndReturn201() throws Exception {
        var request  = new RegisterRequest("acme", "secret");
        var response = new RegisterResponse("acme");

        when(authService.register("acme", "secret")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("acme"));
    }

    @Test
    void shouldReturn409WhenTenantAlreadyExists() throws Exception {
        var request = new RegisterRequest("acme", "secret");

        when(authService.register("acme", "secret"))
                .thenThrow(new TenantAlreadyExistsException("acme"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }
}

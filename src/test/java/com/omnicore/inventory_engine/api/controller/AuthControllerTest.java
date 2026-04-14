package com.omnicore.inventory_engine.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.omnicore.inventory_engine.api.dto.LoginRequest;
import com.omnicore.inventory_engine.api.dto.LoginResponse;
import com.omnicore.inventory_engine.api.dto.RefreshRequest;
import com.omnicore.inventory_engine.api.dto.RegisterRequest;
import com.omnicore.inventory_engine.api.dto.RegisterResponse;
import com.omnicore.inventory_engine.domain.service.AuthService;
import com.omnicore.inventory_engine.domain.service.InvalidCredentialsException;
import com.omnicore.inventory_engine.domain.service.InvalidRefreshTokenException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        var response = new LoginResponse("eyJhbGciOiJIUzI1NiJ9.test.token", "refresh-uuid-token");

        when(authService.login("acme", "secret")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
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
        var request  = new RegisterRequest("acme", "supersecret");
        var response = new RegisterResponse("acme");

        when(authService.register(eq("acme"), eq("supersecret"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("acme"));
    }

    @Test
    void shouldReturn409WhenTenantAlreadyExists() throws Exception {
        var request = new RegisterRequest("acme", "supersecret");

        when(authService.register(eq("acme"), eq("supersecret"), any()))
                .thenThrow(new TenantAlreadyExistsException("acme"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── Validación de entrada ────────────────────────────────────────────────

    @Test
    void shouldReturn400WhenLoginRequestHasBlankTenantId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn400WhenRegisterPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"acme\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── POST /api/v1/auth/refresh ────────────────────────────────────────────

    @Test
    void shouldReturnNewTokenPairWhenRefreshTokenIsValid() throws Exception {
        var request  = new RefreshRequest("valid-refresh-uuid");
        var response = new LoginResponse("new.access.token", "new-refresh-uuid");

        when(authService.refresh("valid-refresh-uuid")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        var request = new RefreshRequest("expired-or-invalid-uuid");

        when(authService.refresh("expired-or-invalid-uuid"))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}

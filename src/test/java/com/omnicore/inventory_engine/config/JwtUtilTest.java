package com.omnicore.inventory_engine.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET =
            "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-algorithm";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET);

    // ─── Test 1: Extrae tenantId de un token válido ────────────────────────────

    @Test
    void shouldExtractTenantIdFromValidToken() {
        String token = jwtUtil.generateToken("tenant-a");

        assertThat(jwtUtil.extractTenantId(token)).isEqualTo("tenant-a");
    }

    // ─── Test 2: Token válido pasa validación ─────────────────────────────────

    @Test
    void shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("tenant-a");

        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    // ─── Test 3: Token con formato inválido falla validación ──────────────────

    @Test
    void shouldReturnFalseForMalformedToken() {
        assertThat(jwtUtil.isValid("not.a.valid.token")).isFalse();
    }

    // ─── Test 4: Token firmado con otro secret falla validación ───────────────

    @Test
    void shouldReturnFalseForTokenWithWrongSecret() {
        JwtUtil otherUtil = new JwtUtil("other-secret-key-also-at-least-256-bits-long-for-hmac");
        String token = otherUtil.generateToken("tenant-a");

        assertThat(jwtUtil.isValid(token)).isFalse();
    }
}

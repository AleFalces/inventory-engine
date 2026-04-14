package com.omnicore.inventory_engine.config;

import com.omnicore.inventory_engine.domain.entity.TenantRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000L; // 15 minutos

    private final SecretKey key;

    public JwtUtil(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String tenantId, TenantRole role) {
        return Jwts.builder()
                .subject(tenantId)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    public String extractTenantId(String token) {
        return parseClaims(token).getSubject();
    }

    public TenantRole extractRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        return TenantRole.valueOf(role);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

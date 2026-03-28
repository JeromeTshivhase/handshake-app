package com.trustmarket.handshake.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpSeconds;
    private final long refreshExpSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-seconds}") long accessExpSeconds,
            @Value("${jwt.refresh-expiration-seconds}") long refreshExpSeconds
    ) {
        // must be 32+ chars for HS256/HS512; keep it long
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpSeconds = accessExpSeconds;
        this.refreshExpSeconds = refreshExpSeconds;
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(Map.of("userId", userId.toString(), "role", role, "type", "ACCESS"), email, accessExpSeconds);
    }

    public String generateRefreshToken(UUID userId, String email, String role) {
        return buildToken(Map.of("userId", userId.toString(), "role", role, "type", "REFRESH"), email, refreshExpSeconds);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
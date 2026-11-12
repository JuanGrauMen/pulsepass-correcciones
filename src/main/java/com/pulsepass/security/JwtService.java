package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${pulsepass.security.jwt.secret-base64}")
            String secretBase64,
            @Value("${pulsepass.security.jwt.issuer}")
            String issuer,
            @Value("${pulsepass.security.jwt.access-token-ttl}")
            Duration accessTokenTtl) {

        byte[] bytes = Decoders.BASE64.decode(secretBase64);

        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 bytes");
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT issuer must not be blank");
        }

        if (accessTokenTtl == null || accessTokenTtl.isZero()
                || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "JWT access token TTL must be positive");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String generateToken(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT subject must not be blank");
        }

        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenTtl);

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Invalid JWT");
        }

        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);

        // La firma se valida antes de confiar en el contenido del token.
        if (!"HS256".equals(parsed.getHeader().getAlgorithm())) {
            throw new JwtException("Invalid JWT algorithm");
        }

        Claims claims = parsed.getPayload();

        // Claims obligatorios exigidos por la especificación del proyecto.
        if (isBlank(claims.getSubject())
                || isBlank(claims.getIssuer())
                || isBlank(claims.getId())
                || claims.getIssuedAt() == null
                || claims.getExpiration() == null) {
            throw new JwtException("Missing required JWT claims");
        }

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        Date now = new Date();

        if (issuedAt.after(now)) {
            throw new JwtException("JWT issued in the future");
        }

        if (!expiration.after(issuedAt)
                || !expiration.after(now)) {
            throw new JwtException("JWT expired or invalid");
        }

        return claims;
    }

    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
package com.pulsepass.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}

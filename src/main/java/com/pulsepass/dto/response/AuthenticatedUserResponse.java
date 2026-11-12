package com.pulsepass.dto.response;

public record AuthenticatedUserResponse(
        Long id,
        String username,
        String email,
        java.util.List<String> roles
) {}

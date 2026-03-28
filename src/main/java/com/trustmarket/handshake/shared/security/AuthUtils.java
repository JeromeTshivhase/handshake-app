package com.trustmarket.handshake.shared.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public class AuthUtils {
    private AuthUtils() {}

    public static UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) return uuid;

        // If some other auth provider sets principal differently
        if (principal instanceof String s) return UUID.fromString(s);

        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }
}
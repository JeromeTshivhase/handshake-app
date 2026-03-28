package com.trustmarket.handshake.auth.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private UUID userId;
    private String email;
    private String role;
    private boolean emailVerified;
    private boolean phoneVerified;

    private String accessToken;
    private String refreshToken;

    private long accessExpiresInSeconds;
    private long refreshExpiresInSeconds;
}
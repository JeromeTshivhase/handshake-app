package com.trustmarket.handshake.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationDispatchResponse {
    private String purpose;
    private String destination;
    private LocalDateTime expiresAt;
    private String message;
}


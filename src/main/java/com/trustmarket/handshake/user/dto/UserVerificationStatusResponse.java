package com.trustmarket.handshake.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVerificationStatusResponse {
    private String email;
    private boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private String phone;
    private boolean phoneVerified;
    private LocalDateTime phoneVerifiedAt;
}


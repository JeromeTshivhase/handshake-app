package com.trustmarket.handshake.safe_meetup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRevealResponse {
    private boolean enabled;
    private String otp;           // only returned when enabled==true
    private LocalDateTime expiresAt;
}
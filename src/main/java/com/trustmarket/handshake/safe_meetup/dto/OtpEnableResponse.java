package com.trustmarket.handshake.safe_meetup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpEnableResponse {
    private boolean enabled;
    private boolean withinTimeWindow;
    private Double distanceMeters;
    private String reason;
    private LocalDateTime otpExpiresAt;
}
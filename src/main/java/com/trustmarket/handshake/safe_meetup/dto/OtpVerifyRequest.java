package com.trustmarket.handshake.safe_meetup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerifyRequest {
    @NotBlank
    private String otp;

    // Optional: send location again to ensure seller is also near
    @NotNull
    private Double lat;

    @NotNull
    private Double lng;
}
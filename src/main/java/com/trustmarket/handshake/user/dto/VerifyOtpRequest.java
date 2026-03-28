package com.trustmarket.handshake.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code")
    private String otp;
}


package com.trustmarket.handshake.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // Optional, required by server when account has 2FA enabled.
    private String twoFactorCode;

    // Optional one-time backup code alternative to TOTP.
    private String recoveryCode;
}
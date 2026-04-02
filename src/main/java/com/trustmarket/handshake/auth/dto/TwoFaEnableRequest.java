package com.trustmarket.handshake.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFaEnableRequest {
    @NotBlank
    private String verificationCode;
}

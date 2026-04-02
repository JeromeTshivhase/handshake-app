package com.trustmarket.handshake.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFaStatusResponse {
    private boolean enabled;
    private String method;
    private String maskedEmail;
}


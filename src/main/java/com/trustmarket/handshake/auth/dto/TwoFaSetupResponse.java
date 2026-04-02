package com.trustmarket.handshake.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFaSetupResponse {
    private String secretKey;
    private String provisioningUri;
    private String qrCodeUrl;
}


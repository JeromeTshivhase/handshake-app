package com.trustmarket.handshake.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutMethodDto {
    private UUID id;
    private String bankName;
    private String branchCode;
    private String accountNumber;
    private String identityFingerprint;
}


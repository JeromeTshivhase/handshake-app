package com.trustmarket.handshake.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull
    private UUID handshakeId;

    @NotBlank
    private String returnUrl;
}
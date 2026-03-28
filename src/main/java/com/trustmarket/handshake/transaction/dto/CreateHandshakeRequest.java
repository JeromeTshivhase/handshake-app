package com.trustmarket.handshake.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHandshakeRequest {

    @NotNull
    private UUID listingId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal agreedPrice;

    private LocalDateTime expiresAt;
}
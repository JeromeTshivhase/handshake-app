package com.trustmarket.handshake.transaction.dto;

import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandshakeResponse {
    private UUID id;
    private UUID listingId;
    private UUID buyerId;
    private UUID sellerId;
    private BigDecimal agreedPrice;
    private HandshakeStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private TransactionStatus transactionStatus;

    public static HandshakeResponse from(Handshake h, TransactionStatus transactionStatus) {
        return HandshakeResponse.builder()
                .id(h.getId())
                .listingId(h.getListing().getId())
                .buyerId(h.getBuyer().getId())
                .sellerId(h.getSeller().getId())
                .agreedPrice(h.getAgreedPrice())
                .status(h.getStatus())
                .expiresAt(h.getExpiresAt())
                .createdAt(h.getCreatedAt())
                .transactionStatus(transactionStatus)
                .build();
    }

    public static HandshakeResponse from(Handshake h) {
        return from(h, null);
    }
}
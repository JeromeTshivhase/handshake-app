package com.trustmarket.handshake.transaction.dto;

import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private UUID handshakeId;
    private UUID listingId;
    private UUID buyerId;
    private UUID sellerId;

    private BigDecimal amount;
    private String currency;

    private TransactionStatus status;
    private LocalDateTime escrowHeldAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .handshakeId(t.getHandshake().getId())
                .listingId(t.getListing().getId())
                .buyerId(t.getBuyer().getId())
                .sellerId(t.getSeller().getId())
                .amount(t.getAmount())
                .currency(t.getCurrency().name())
                .status(t.getStatus())
                .escrowHeldAt(t.getEscrowHeldAt())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
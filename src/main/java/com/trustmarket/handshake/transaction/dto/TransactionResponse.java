package com.trustmarket.handshake.transaction.dto;

import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.PaymentStatus;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.enums.TransactionSystemAction;
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
    private PaymentStatus paymentStatus;
    private String paymentReference;

    private LocalDateTime escrowHeldAt;
    private LocalDateTime escrowReleaseAt;
    private boolean escrowHoldActive;
    private boolean escrowReleasableNow;
    private Long escrowHoldRemainingMinutes;
    private TransactionSystemAction lastSystemAction;
    private LocalDateTime lastSystemActionAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime releaseAt = t.getEscrowReleaseAt();
        boolean holdActive = releaseAt != null && now.isBefore(releaseAt);
        long remainingMinutes = holdActive ? java.time.Duration.between(now, releaseAt).toMinutes() : 0;

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
                .escrowReleaseAt(releaseAt)
                .escrowHoldActive(holdActive)
                .escrowReleasableNow(!holdActive && t.getStatus() == TransactionStatus.ESCROW_HELD)
                .escrowHoldRemainingMinutes(remainingMinutes)
                .lastSystemAction(t.getLastSystemAction())
                .lastSystemActionAt(t.getLastSystemActionAt())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .build();
    }

    public static TransactionResponse from(Transaction t, PaymentStatus paymentStatus, String paymentReference) {
        TransactionResponse r = from(t);
        r.setPaymentStatus(paymentStatus);
        r.setPaymentReference(paymentReference);
        return r;
    }
}
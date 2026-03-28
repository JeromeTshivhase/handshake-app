package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final NotificationPublisher notificationPublisher;
    private final HandshakeRepository handshakeRepository;

    @Transactional
    public Transaction createFromAcceptedHandshake(Handshake handshake) {
        Listing listing = handshake.getListing();

        Transaction txn = Transaction.builder()
                .handshake(handshake)
                .listing(listing)
                .buyer(handshake.getBuyer())
                .seller(handshake.getSeller())
                .amount(handshake.getAgreedPrice())
                .status(TransactionStatus.INITIATED)
                .build();

        return transactionRepository.save(txn);
    }

    @Transactional
    public void markPaymentPending(Transaction txn) {
        txn.setStatus(TransactionStatus.PAYMENT_PENDING);
        transactionRepository.save(txn);
    }

    @Transactional
    public void markEscrowHeld(Transaction txn) {
        txn.setStatus(TransactionStatus.ESCROW_HELD);
        txn.setEscrowHeldAt(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    /**
     * Buyer confirms meetup/delivery completed:
     * ESCROW_HELD -> COMPLETED
     * Listing ACTIVE -> SOLD
     */
    @Transactional
    public Transaction complete(UUID transactionId, UUID buyerId) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!txn.getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenException("Only the buyer can complete the transaction");
        }

        if (txn.getStatus() != TransactionStatus.ESCROW_HELD) {
            throw new BadRequestException("Transaction must be ESCROW_HELD to complete. Current=" + txn.getStatus());
        }

        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        txn = transactionRepository.save(txn);

        // Mark listing as SOLD (it may have been ACTIVE, or still shown in search up to now)
        Listing listing = txn.getListing();
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        // Mark handshake as COMPLETED
        Handshake handshake = txn.getHandshake();
        handshake.setStatus(HandshakeStatus.COMPLETED);
        handshakeRepository.save(handshake);

        notificationPublisher.publishTransactionCompleted(txn);

        return txn;
    }

    @Transactional(readOnly = true)
    public Transaction get(UUID transactionId, UUID userId) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        boolean allowed = txn.getBuyer().getId().equals(userId) || txn.getSeller().getId().equals(userId);
        if (!allowed) throw new ForbiddenException("Not allowed");

        return txn;
    }

    @Transactional
    public void markDisputed(UUID transactionId, String reason) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
        txn.setStatus(TransactionStatus.DISPUTED);
        transactionRepository.save(txn);
        Handshake handshake = txn.getHandshake();
        handshake.setStatus(HandshakeStatus.DISPUTED);
        handshakeRepository.save(handshake);
        // Optionally store reason in a new field or log
    }

    @Transactional
    public void refundToBuyer(UUID transactionId) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
        if (txn.getStatus() != TransactionStatus.ESCROW_HELD && txn.getStatus() != TransactionStatus.DISPUTED) {
            throw new BadRequestException("Only ESCROW_HELD or DISPUTED transactions can be refunded");
        }
        txn.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(txn);
        Handshake handshake = txn.getHandshake();
        handshake.setStatus(HandshakeStatus.CANCELLED);
        handshakeRepository.save(handshake);
        // TODO: Integrate with payment provider to actually move funds
    }
}
package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
import com.trustmarket.handshake.safe_meetup.repository.MeetupRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.enums.TransactionSystemAction;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final NotificationPublisher notificationPublisher;
    private final HandshakeRepository handshakeRepository;
    private final MeetupRepository meetupRepository;

    /**
     * Allowed status transitions.
     * Terminal states (COMPLETED, REFUNDED) have no outbound edges.
     */
    private static final Map<TransactionStatus, Set<TransactionStatus>> VALID_TRANSITIONS = Map.of(
            TransactionStatus.INITIATED,       EnumSet.of(TransactionStatus.PAYMENT_PENDING, TransactionStatus.CANCELLED),
            TransactionStatus.PAYMENT_PENDING, EnumSet.of(TransactionStatus.ESCROW_HELD, TransactionStatus.CANCELLED),
            TransactionStatus.ESCROW_HELD,     EnumSet.of(TransactionStatus.COMPLETED, TransactionStatus.DISPUTED, TransactionStatus.REFUNDED),
            TransactionStatus.DISPUTED,        EnumSet.of(TransactionStatus.REFUNDED)
    );

    private void validateTransition(TransactionStatus from, TransactionStatus to) {
        Set<TransactionStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BadRequestException(
                    "Invalid transaction status transition from " + from + " to " + to);
        }
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
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
        validateTransition(txn.getStatus(), TransactionStatus.PAYMENT_PENDING);
        txn.setStatus(TransactionStatus.PAYMENT_PENDING);
        txn.setLastSystemAction(TransactionSystemAction.PAYMENT_INITIATED);
        txn.setLastSystemActionAt(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    @Transactional
    public void markEscrowHeld(Transaction txn) {
        validateTransition(txn.getStatus(), TransactionStatus.ESCROW_HELD);
        txn.setStatus(TransactionStatus.ESCROW_HELD);
        txn.setEscrowHeldAt(LocalDateTime.now());
        txn.setLastSystemAction(TransactionSystemAction.PAYMENT_COMPLETED);
        txn.setLastSystemActionAt(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    /**
     * Buyer confirms meetup/delivery completed.
     * For meetup-backed transactions the OTP must have been verified first.
     * ESCROW_HELD -> COMPLETED
     */
    @Transactional
    public Transaction complete(UUID transactionId, UUID buyerId) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!txn.getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenException("Only the buyer can complete the transaction");
        }

        validateTransition(txn.getStatus(), TransactionStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        if (txn.getEscrowReleaseAt() != null && now.isBefore(txn.getEscrowReleaseAt())) {
            throw new BadRequestException("Escrow hold is active until " + txn.getEscrowReleaseAt());
        }
        txn.setLastSystemAction(TransactionSystemAction.MANUAL_COMPLETED);
        txn.setLastSystemActionAt(now);

        // Require OTP to be verified when a meetup exists for this handshake.
        meetupRepository.findByHandshake_Id(txn.getHandshake().getId())
                .ifPresent(meetup -> {
                    if (meetup.getStatus() != MeetupStatus.COMPLETED) {
                        throw new BadRequestException(
                                "OTP must be verified at the meetup location before completing the transaction");
                    }
                });

        return finalizeCompletion(txn, now);
    }

    @Transactional
    public boolean autoReleaseEscrowIfDue(UUID transactionId) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (txn.getStatus() != TransactionStatus.ESCROW_HELD) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (txn.getEscrowReleaseAt() != null && now.isBefore(txn.getEscrowReleaseAt())) {
            return false;
        }

        // If meetup exists, do not auto-release before OTP flow marks meetup complete.
        boolean meetupCompleteOrAbsent = meetupRepository.findByHandshake_Id(txn.getHandshake().getId())
                .map(meetup -> meetup.getStatus() == MeetupStatus.COMPLETED)
                .orElse(true);
        if (!meetupCompleteOrAbsent) {
            log.warn("Escrow release skipped for txn {} because meetup is not completed", txn.getId());
            return false;
        }

        txn.setLastSystemAction(TransactionSystemAction.AUTO_RELEASED);
        txn.setLastSystemActionAt(now);

        Transaction completed = finalizeCompletion(txn, now);
        notificationPublisher.publishEscrowReleased(completed);
        return true;
    }

    private Transaction finalizeCompletion(Transaction txn, LocalDateTime completedAt) {
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(completedAt);
        txn = transactionRepository.save(txn);

        Listing listing = txn.getListing();
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

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

        validateTransition(txn.getStatus(), TransactionStatus.DISPUTED);
        txn.setStatus(TransactionStatus.DISPUTED);
        txn.setLastSystemAction(TransactionSystemAction.MANUALLY_DISPUTED);
        txn.setLastSystemActionAt(LocalDateTime.now());
        transactionRepository.save(txn);

        if (reason != null && !reason.isBlank()) {
            log.info("Transaction {} disputed. Reason: {}", transactionId, reason);
        }

        Handshake handshake = txn.getHandshake();
        handshake.setStatus(HandshakeStatus.DISPUTED);
        handshakeRepository.save(handshake);
    }

    @Transactional
    public void refundToBuyer(UUID transactionId) {
        Transaction txn = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        validateTransition(txn.getStatus(), TransactionStatus.REFUNDED);
        txn.setLastSystemAction(TransactionSystemAction.ADMIN_REFUNDED);
        txn.setLastSystemActionAt(LocalDateTime.now());

        // TODO: call PayFast refund API here before persisting REFUNDED status
        // payFastService.issueRefund(payment.getPaymentReference(), txn.getAmount());

        txn.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(txn);

        Handshake handshake = txn.getHandshake();
        handshake.setStatus(HandshakeStatus.CANCELLED);
        handshakeRepository.save(handshake);
    }
}
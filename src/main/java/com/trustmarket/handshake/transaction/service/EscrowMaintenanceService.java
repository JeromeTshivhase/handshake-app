package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowMaintenanceService {

    private final HandshakeRepository handshakeRepository;
    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Value("${handshake.pending-expiry-hours:48}")
    private int pendingExpiryHours;

    @Value("${handshake.accepted-unpaid-expiry-hours:24}")
    private int acceptedUnpaidExpiryHours;

    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirePendingHandshakes() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(pendingExpiryHours);
        List<Handshake> expired = handshakeRepository.findAllByStatusAndCreatedAtBefore(HandshakeStatus.PENDING, cutoff);
        for (Handshake h : expired) {
            h.setStatus(HandshakeStatus.EXPIRED);
            handshakeRepository.save(h);
            log.info("Auto-expired PENDING handshake {}", h.getId());
        }
    }

    /** Cancel ACCEPTED handshakes that have not received payment within the configured window. */
    // Run every hour
    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void cancelUnpaidAcceptedHandshakes() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(acceptedUnpaidExpiryHours);
        List<Handshake> toCancel = handshakeRepository.findAllByStatusAndUpdatedAtBefore(HandshakeStatus.ACCEPTED, cutoff);
        for (Handshake h : toCancel) {
            h.setStatus(HandshakeStatus.CANCELLED);
            handshakeRepository.save(h);
            Listing listing = h.getListing();
            if (listing.getStatus() == ListingStatus.PENDING_PAYMENT) {
                listing.setStatus(ListingStatus.ACTIVE);
                listingRepository.save(listing);
            }
            log.info("Auto-cancelled unpaid ACCEPTED handshake {} and reactivated listing {}",
                    h.getId(), listing.getId());
        }
    }

    // Run every 10 minutes.
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void releaseHeldEscrows() {
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> readyTransactions = transactionRepository
                .findAllByStatusAndEscrowReleaseAtLessThanEqual(TransactionStatus.ESCROW_HELD, now);

        for (Transaction txn : readyTransactions) {
            boolean released = transactionService.autoReleaseEscrowIfDue(txn.getId());
            if (released) {
                log.info("Auto-released escrow for transaction {}", txn.getId());
            }
        }
    }
}

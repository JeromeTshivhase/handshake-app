package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirePendingHandshakes() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<Handshake> expired = handshakeRepository.findAllByStatusAndCreatedAtBefore(HandshakeStatus.PENDING, cutoff);
        for (Handshake h : expired) {
            h.setStatus(HandshakeStatus.EXPIRED);
            handshakeRepository.save(h);
            log.info("Auto-expired handshake {}", h.getId());
        }
    }

    // Run every hour
    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void cancelUnpaidAcceptedHandshakes() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<Handshake> toCancel = handshakeRepository.findAllByStatusAndUpdatedAtBefore(HandshakeStatus.ACCEPTED, cutoff);
        for (Handshake h : toCancel) {
            h.setStatus(HandshakeStatus.CANCELLED);
            handshakeRepository.save(h);
            Listing listing = h.getListing();
            if (listing.getStatus() == ListingStatus.PENDING_PAYMENT) {
                listing.setStatus(ListingStatus.ACTIVE);
                listingRepository.save(listing);
            }
            log.info("Auto-cancelled handshake {} and reactivated listing {}", h.getId(), listing.getId());
        }
    }
}

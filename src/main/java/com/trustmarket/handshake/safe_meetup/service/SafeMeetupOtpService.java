package com.trustmarket.handshake.safe_meetup.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.safe_meetup.dto.OtpEnableResponse;
import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.safe_meetup.repository.MeetupRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SafeMeetupOtpService {

    private final HandshakeRepository handshakeRepository;
    private final MeetupRepository meetupRepository;
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final MeetupValidationService validationService;
    private final OtpService otpService;
    private final NotificationPublisher notificationPublisher;

    @Value("${otp.expires-minutes:10}")
    private long otpExpiresMinutes;

    @Transactional
    public OtpEnableResponse enableIfEligible(UUID handshakeId, UUID userId, double lat, double lng) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        boolean allowed = h.getBuyer().getId().equals(userId) || h.getSeller().getId().equals(userId);
        if (!allowed) throw new ForbiddenException("Not allowed");

        if (h.getStatus() != HandshakeStatus.ACCEPTED && h.getStatus() != HandshakeStatus.ESCROW_HELD) {
            throw new BadRequestException("OTP can only be enabled after handshake ACCEPTED or ESCROW_HELD");
        }

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElse(null);

        List<String> reasons = new ArrayList<>();
        if (meetup == null) reasons.add("MEETUP_NOT_SCHEDULED");

        boolean within = false;
        double dist = Double.NaN;

        if (meetup != null) {
            within = validationService.withinTimeWindow(LocalDateTime.now(), meetup.getScheduledTime());
            if (!within) reasons.add("NOT_WITHIN_TIME_WINDOW");

            dist = validationService.distanceMeters(lat, lng, meetup.getLatitude(), meetup.getLongitude());
            if (dist > validationService.radiusMeters()) reasons.add("TOO_FAR");
        }

        boolean enabled = reasons.isEmpty();

        if (enabled) {
            h.setOtpEnabled(true);
            // If otp expired or missing, extend expiry window (OTP itself generated at accept and stored as hash)
            if (h.getOtpExpiresAt() == null || h.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
                h.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiresMinutes));
            }
            handshakeRepository.save(h);
        }

        return OtpEnableResponse.builder()
                .enabled(enabled)
                .distanceMeters(Double.isNaN(dist) ? -1 : dist)
                .withinTimeWindow(within)
                .reasons(reasons)
                .build();
    }

    @Transactional(readOnly = true)
    public String revealOtpToBuyer(UUID handshakeId, UUID buyerId, String plainOtpFromAcceptStore) {
        // NOTE: we do NOT store plain OTP in DB.
        // Since you requested generate OTP on accept but reveal later, we need a place to store plaintext securely.
        // Best approach: store plaintext OTP in Redis with TTL at accept time.
        // For now, this method signature requires you to supply the plaintext from Redis store.
        // We'll implement Redis storage next (recommended).

        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        if (!h.getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenException("Only buyer can view OTP");
        }

        if (!h.isOtpEnabled()) {
            throw new BadRequestException("OTP not enabled yet (arrive near meetup at the scheduled time)");
        }

        if (h.getOtpExpiresAt() != null && h.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired. Re-enable OTP at the meetup location to refresh.");
        }

        return plainOtpFromAcceptStore;
    }

    @Transactional
    public void verifyOtpBySeller(UUID handshakeId, UUID sellerId, String otp, double lat, double lng) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        if (!h.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("Only seller can verify OTP");
        }

        if (h.getStatus() != HandshakeStatus.ESCROW_HELD) {
            throw new BadRequestException("Handshake must be ACCEPTED");
        }

        if (!h.isOtpEnabled()) {
            throw new BadRequestException("OTP not enabled yet");
        }

        if (h.isOtpLockedNow()) {
            throw new BadRequestException("OTP verification locked until " + h.getOtpLockedUntil());
        }

        if (h.getOtpExpiresAt() != null && h.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired");
        }

        // Optional: ensure seller also near
        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new BadRequestException("Meetup not scheduled"));

        boolean within = validationService.withinTimeWindow(LocalDateTime.now(), meetup.getScheduledTime());
        double dist = validationService.distanceMeters(lat, lng, meetup.getLatitude(), meetup.getLongitude());

        if (!within) throw new BadRequestException("Not within meetup time window");
        if (dist > validationService.radiusMeters()) throw new BadRequestException("Too far from meetup location");

        // Verify OTP
        if (h.getOtpCodeHash() == null) {
            throw new BadRequestException("OTP not initialized");
        }

        boolean ok = otpService.matches(otp, h.getOtpCodeHash());
        if (!ok) {
            int attempts = h.getOtpFailedAttempts() + 1;
            h.setOtpFailedAttempts(attempts);

            if (attempts >= 5) {
                h.setOtpLockedUntil(LocalDateTime.now().plusMinutes(15));
            }
            handshakeRepository.save(h);
            throw new BadRequestException("Invalid OTP");
        }

        // Mark verified
        h.setOtpVerified(true);
        h.setOtpVerifiedAt(LocalDateTime.now());
        h.setOtpFailedAttempts(0);
        h.setOtpLockedUntil(null);
        // Set handshake status to COMPLETED
        h.setStatus(HandshakeStatus.COMPLETED);
        handshakeRepository.save(h);

        // Complete transaction and mark listing sold
        Transaction txn = transactionRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new BadRequestException("Transaction not found for handshake"));

        if (txn.getStatus() != TransactionStatus.ESCROW_HELD) {
            throw new BadRequestException("Transaction must be ESCROW_HELD to complete via OTP. Current=" + txn.getStatus());
        }

        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(txn);

        Listing listing = txn.getListing();
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        // Publish notification for transaction completed
        notificationPublisher.publishTransactionCompleted(txn);
    }
}


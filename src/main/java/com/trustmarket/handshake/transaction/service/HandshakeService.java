package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.safe_meetup.service.OtpRedisStore;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.transaction.dto.CreateHandshakeRequest;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HandshakeService {

    @Value("${handshake.min-expiry-lead-seconds:30}")
    private int minExpiryLeadSeconds;

    @Value("${handshake.expiry-grace-seconds:30}")
    private int expiryGraceSeconds;

    private final HandshakeRepository handshakeRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final OtpRedisStore otpRedisStore;
    private final NotificationPublisher notificationPublisher;

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Handshake> findByUserIdAndStatus(UUID userId, HandshakeStatus status, org.springframework.data.domain.Pageable pageable) {
        return handshakeRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * Open market handshake: Listings remain ACTIVE as long as no confirmed sale,
     * multiple concurrent handshakes allowed.
     */
    @Transactional
    public Handshake create(UUID buyerId, CreateHandshakeRequest req) {
        LocalDateTime now = LocalDateTime.now();

        if (req.getExpiresAt() != null && !req.getExpiresAt().isAfter(now.plusSeconds(minExpiryLeadSeconds))) {
            throw new BadRequestException("Handshake expiry must be at least " + minExpiryLeadSeconds + " seconds in the future");
        }

        Listing listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> new NotFoundException("Listing not found: " + req.getListingId()));

        if (listing.getSeller().getId().equals(buyerId)) {
            throw new BadRequestException("You cannot buy your own listing");
        }

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Listing is not available. Status=" + listing.getStatus());
        }

        // Prevent duplicate handshake for same listing and buyer if active
        var activeStatuses = java.util.Arrays.asList(HandshakeStatus.PENDING, HandshakeStatus.ACCEPTED);
        boolean exists = !handshakeRepository.findByListing_IdAndBuyer_IdAndStatusIn(listing.getId(), buyerId, activeStatuses).isEmpty();
        if (exists) {
            throw new BadRequestException("You already have an active handshake for this listing");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("Buyer not found: " + buyerId));
        User seller = listing.getSeller();

        Handshake handshake = Handshake.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .agreedPrice(req.getAgreedPrice())
                .status(HandshakeStatus.PENDING)
                .expiresAt(req.getExpiresAt())
                .otpEnabled(false)
                .otpVerified(false)
                .otpVerifiedAt(null)
                .otpExpiresAt(null)
                .otpCodeHash(null)
                .otpFailedAttempts(0)
                .otpLockedUntil(null)
                .build();

        handshake = handshakeRepository.save(handshake);

        // DO NOT CHANGE LISTING STATUS HERE! It MUST remain ACTIVE until sold or deactivated.
        notificationPublisher.publishHandshakeRequest(handshake);
        return handshake;
    }

    @Transactional
    public Handshake accept(UUID sellerId, UUID handshakeId) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        LocalDateTime now = LocalDateTime.now();

        if (!h.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("Only the seller can accept this handshake");
        }

        if (h.getStatus() != HandshakeStatus.PENDING) {
            throw new BadRequestException("Handshake is not pending");
        }

        // Small grace period avoids race/time-skew edge cases right after creation.
        if (h.getExpiresAt() != null && h.getExpiresAt().isBefore(now.minusSeconds(expiryGraceSeconds))) {
            h.setStatus(HandshakeStatus.EXPIRED);
            handshakeRepository.save(h);
            // Listing remains ACTIVE (not unlocked from PENDING)
            throw new BadRequestException("Handshake expired");
        }

        h.setStatus(HandshakeStatus.ACCEPTED);

        // Set listing status to PENDING_PAYMENT
        Listing listing = h.getListing();
        listing.setStatus(ListingStatus.PENDING_PAYMENT);
        listingRepository.save(listing);

        h.setOtpCodeHash(null);
        h.setOtpRevealCiphertext(null);
        h.setOtpEnabled(false);
        h.setOtpVerified(false);
        h.setOtpVerifiedAt(null);
        h.setOtpExpiresAt(null);
        h.setOtpFailedAttempts(0);
        h.setOtpLockedUntil(null);
        h = handshakeRepository.save(h);
        transactionService.createFromAcceptedHandshake(h);
        notificationPublisher.publishHandshakeAccepted(h);
        return h;
    }

    @Transactional
    public Handshake reject(UUID sellerId, UUID handshakeId) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        if (!h.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("Only the seller can reject this handshake");
        }

        if (h.getStatus() != HandshakeStatus.PENDING) {
            throw new BadRequestException("Handshake is not pending");
        }

        h.setStatus(HandshakeStatus.REJECTED);
        h = handshakeRepository.save(h);

        // Listing remains ACTIVE
        otpRedisStore.delete(h.getId());
        notificationPublisher.publishHandshakeRejected(h);
        return h;
    }

    @Transactional
    public Handshake cancel(UUID userId, UUID handshakeId) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        boolean participant = h.getBuyer().getId().equals(userId) || h.getSeller().getId().equals(userId);
        if (!participant) throw new ForbiddenException("Not allowed");

        if (h.getStatus() != HandshakeStatus.PENDING) {
            throw new BadRequestException("Only pending handshakes can be cancelled");
        }

        h.setStatus(HandshakeStatus.CANCELLED);
        h = handshakeRepository.save(h);

        // Listing remains ACTIVE
        otpRedisStore.delete(h.getId());
        notificationPublisher.publishHandshakeCancelled(h);
        return h;
    }

    @Transactional(readOnly = true)
    public Handshake get(UUID handshakeId, UUID userId) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        if (!h.getBuyer().getId().equals(userId) && !h.getSeller().getId().equals(userId)) {
            throw new ForbiddenException("Not allowed");
        }

        return h;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Handshake> findByBuyerIdAndStatus(UUID buyerId, HandshakeStatus status, org.springframework.data.domain.Pageable pageable) {
        return handshakeRepository.findByBuyerIdAndStatus(buyerId, status, pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Handshake> findBySellerIdAndStatus(UUID sellerId, HandshakeStatus status, org.springframework.data.domain.Pageable pageable) {
        return handshakeRepository.findBySellerIdAndStatus(sellerId, status, pageable);
    }
}
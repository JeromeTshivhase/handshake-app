package com.trustmarket.handshake.safe_meetup.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.safe_meetup.dto.CheckinRequest;
import com.trustmarket.handshake.safe_meetup.dto.OtpEnableResponse;
import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafeMeetupOtpService {

    private final HandshakeRepository handshakeRepository;
    private final MeetupRepository meetupRepository;
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final MeetupValidationService validationService;
    private final OtpService otpService;
    private final OtpRedisStore otpRedisStore;
    private final OtpRevealCryptoService otpRevealCryptoService;
    private final NotificationPublisher notificationPublisher;

    @Value("${otp.expires-minutes:10}")
    private long otpExpiresMinutes;

    @Value("${otp.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${otp.lock-minutes:15}")
    private int otpLockMinutes;

    @Transactional
    public OtpEnableResponse enableIfEligible(UUID handshakeId, UUID userId, double lat, double lng) {
        Handshake handshake = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        if (!handshake.getBuyer().getId().equals(userId)) {
            throw new ForbiddenException("Only the buyer can enable OTP");
        }

        if (handshake.getStatus() != HandshakeStatus.ESCROW_HELD) {
            throw new BadRequestException("OTP can only be enabled when handshake is ESCROW_HELD");
        }

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new BadRequestException("Meetup not scheduled"));

        if (meetup.getStatus() == MeetupStatus.COMPLETED) {
            throw new BadRequestException("Meetup already completed");
        }
        if (meetup.getStatus() == MeetupStatus.CANCELLED) {
            throw new BadRequestException("Meetup was cancelled");
        }
        if (meetup.getStatus() == MeetupStatus.EXPIRED) {
            throw new BadRequestException("Meetup has expired");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean withinTimeWindow = meetup.isWithinTimeWindow(now);
        double distanceMeters = validationService.distanceMeters(lat, lng, meetup.getLatitude(), meetup.getLongitude());

        if (!withinTimeWindow) {
            meetup.setStatus(MeetupStatus.SCHEDULED);
            meetupRepository.save(meetup);
            return OtpEnableResponse.builder()
                    .enabled(false)
                    .withinTimeWindow(false)
                    .distanceMeters(distanceMeters)
                    .reason("NOT_WITHIN_TIME_WINDOW")
                    .build();
        }

        if (distanceMeters > meetup.getRadiusMeters()) {
            meetup.setStatus(MeetupStatus.READY);
            meetupRepository.save(meetup);
            return OtpEnableResponse.builder()
                    .enabled(false)
                    .withinTimeWindow(true)
                    .distanceMeters(distanceMeters)
                    .reason("TOO_FAR")
                    .build();
        }

        if (meetup.isOtpEnabled() && !meetup.isOtpExpired()) {
            return OtpEnableResponse.builder()
                    .enabled(true)
                    .withinTimeWindow(true)
                    .distanceMeters(distanceMeters)
                    .otpExpiresAt(meetup.getOtpExpiresAt())
                    .build();
        }

        String otp = otpService.generate6Digits();
        String otpHash = otpService.hash(otp);

        meetup.setOtpCodeHash(otpHash);
        meetup.setOtpEnabledAt(now);
        meetup.setOtpExpiresAt(now.plusMinutes(otpExpiresMinutes));
        meetup.setOtpFailedAttempts(0);
        meetup.setOtpLockedUntil(null);
        meetup.setOtpVerifiedAt(null);
        meetup.setStatus(MeetupStatus.ACTIVE);
        meetup.setBuyerLastLatitude(lat);
        meetup.setBuyerLastLongitude(lng);
        meetup.setBuyerLastCheckin(now);
        meetupRepository.save(meetup);

        handshake.setOtpCodeHash(otpHash);
        handshake.setOtpEnabled(true);
        handshake.setOtpVerified(false);
        handshake.setOtpVerifiedAt(null);
        handshake.setOtpExpiresAt(meetup.getOtpExpiresAt());
        handshake.setOtpFailedAttempts(0);
        handshake.setOtpLockedUntil(null);
        handshake.setOtpRevealCiphertext(otpRevealCryptoService.encrypt(handshake.getId(), otp));
        handshakeRepository.save(handshake);

        otpRedisStore.put(handshakeId, otp, Duration.ofMinutes(otpExpiresMinutes));

        return OtpEnableResponse.builder()
                .enabled(true)
                .withinTimeWindow(true)
                .distanceMeters(distanceMeters)
                .otpExpiresAt(meetup.getOtpExpiresAt())
                .build();
    }

    @Transactional(readOnly = true)
    public String revealOtpToBuyer(UUID handshakeId, UUID buyerId, String ignoredPlainOtpFromAcceptStore) {
        Handshake handshake = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        if (!handshake.getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenException("Only buyer can view OTP");
        }

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new BadRequestException("Meetup not scheduled"));

        if (meetup.getStatus() != MeetupStatus.ACTIVE || !meetup.isOtpEnabled()) {
            throw new BadRequestException("OTP is not enabled yet");
        }

        if (meetup.isOtpExpired()) {
            throw new BadRequestException("OTP expired. Re-enable OTP at the meetup location.");
        }

        return getRevealableOtp(handshake);
    }

    @Transactional
    public void verifyOtpBySeller(UUID handshakeId, UUID sellerId, String otp, double lat, double lng) {
        Handshake handshake = handshakeRepository.findByIdForOtpVerification(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        Meetup meetup = meetupRepository.findByHandshakeIdForUpdate(handshakeId)
                .orElseThrow(() -> new BadRequestException("Meetup not scheduled"));

        if (!handshake.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("Only seller can verify OTP");
        }
        if (handshake.getStatus() != HandshakeStatus.ESCROW_HELD) {
            throw new BadRequestException("Handshake must be ESCROW_HELD");
        }
        if (meetup.getStatus() == MeetupStatus.COMPLETED) {
            throw new BadRequestException("Meetup already completed");
        }
        if (meetup.getStatus() != MeetupStatus.ACTIVE) {
            throw new BadRequestException("OTP is not active");
        }
        if (meetup.isOtpLocked() || handshake.isOtpLockedNow()) {
            LocalDateTime lockedUntil = meetup.getOtpLockedUntil() != null ? meetup.getOtpLockedUntil() : handshake.getOtpLockedUntil();
            throw new BadRequestException("Too many failed attempts. Try again after " + lockedUntil);
        }
        if (meetup.isOtpExpired()) {
            meetup.setStatus(MeetupStatus.EXPIRED);
            meetupRepository.save(meetup);
            throw new BadRequestException("OTP has expired. Buyer must re-enable OTP.");
        }
        if (!meetup.isWithinTimeWindow(LocalDateTime.now())) {
            meetup.setStatus(MeetupStatus.EXPIRED);
            meetupRepository.save(meetup);
            throw new BadRequestException("Not within meetup time window");
        }

        double sellerDistance = validationService.distanceMeters(lat, lng, meetup.getLatitude(), meetup.getLongitude());
        if (sellerDistance > meetup.getRadiusMeters()) {
            throw new BadRequestException("Seller is too far from meetup location");
        }

        meetup.setSellerLastLatitude(lat);
        meetup.setSellerLastLongitude(lng);
        meetup.setSellerLastCheckin(LocalDateTime.now());

        boolean ok = otpService.matches(otp, meetup.getOtpCodeHash());
        if (!ok) {
            int failedAttempts = (meetup.getOtpFailedAttempts() == null ? 0 : meetup.getOtpFailedAttempts()) + 1;
            meetup.setOtpFailedAttempts(failedAttempts);
            handshake.setOtpFailedAttempts(failedAttempts);

            if (failedAttempts >= maxFailedAttempts) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(otpLockMinutes);
                meetup.setOtpLockedUntil(lockUntil);
                handshake.setOtpLockedUntil(lockUntil);
            }

            meetupRepository.save(meetup);
            handshakeRepository.save(handshake);
            throw new BadRequestException("Invalid OTP");
        }

        LocalDateTime verifiedAt = LocalDateTime.now();

        meetup.setOtpVerifiedAt(verifiedAt);
        meetup.setOtpFailedAttempts(0);
        meetup.setOtpLockedUntil(null);
        meetup.setStatus(MeetupStatus.COMPLETED);
        meetupRepository.save(meetup);

        handshake.setOtpVerified(true);
        handshake.setOtpVerifiedAt(verifiedAt);
        handshake.setOtpEnabled(false);
        handshake.setOtpFailedAttempts(0);
        handshake.setOtpLockedUntil(null);
        handshake.setOtpRevealCiphertext(null);
        handshake.setStatus(HandshakeStatus.COMPLETED);
        handshakeRepository.save(handshake);

        otpRedisStore.delete(handshakeId);

        Transaction txn = transactionRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new BadRequestException("Transaction not found for handshake"));

        if (txn.getStatus() != TransactionStatus.ESCROW_HELD) {
            throw new BadRequestException("Transaction must be ESCROW_HELD to complete via OTP");
        }

        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(verifiedAt);
        transactionRepository.save(txn);

        Listing listing = txn.getListing();
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        notificationPublisher.publishTransactionCompleted(txn);
    }

    @Transactional
    public void checkin(UUID handshakeId, UUID userId, CheckinRequest request) {
        Handshake handshake = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        boolean isBuyer = handshake.getBuyer().getId().equals(userId);
        boolean isSeller = handshake.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("Not allowed");
        }

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new BadRequestException("Meetup not scheduled"));

        LocalDateTime now = LocalDateTime.now();
        if (isBuyer) {
            meetup.setBuyerLastLatitude(request.getLat());
            meetup.setBuyerLastLongitude(request.getLng());
            meetup.setBuyerLastCheckin(now);
        } else {
            meetup.setSellerLastLatitude(request.getLat());
            meetup.setSellerLastLongitude(request.getLng());
            meetup.setSellerLastCheckin(now);
        }

        meetupRepository.save(meetup);
    }

    private String getRevealableOtp(Handshake handshake) {
        Optional<String> redisOtp = otpRedisStore.get(handshake.getId());
        if (redisOtp.isPresent()) {
            return redisOtp.get();
        }

        if (handshake.getOtpRevealCiphertext() == null || handshake.getOtpRevealCiphertext().isBlank()) {
            throw new BadRequestException("OTP not available. Re-enable OTP at meetup location.");
        }

        return otpRevealCryptoService.decrypt(handshake.getId(), handshake.getOtpRevealCiphertext());
    }
}

package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.safe_meetup.service.OtpService;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.dto.UserVerificationStatusResponse;
import com.trustmarket.handshake.user.dto.VerificationDispatchResponse;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.VerificationOtpChallenge;
import com.trustmarket.handshake.user.entity.VerificationPurpose;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.repository.VerificationOtpChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserVerificationService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final VerificationOtpChallengeRepository challengeRepository;
    private final VerificationNotificationService notificationService;
    private final OtpService otpService;

    @Value("${app.verification.otp.expires-minutes:10}")
    private long otpExpiresMinutes;

    @Value("${app.verification.otp.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.verification.otp.lock-minutes:15}")
    private long lockMinutes;

    @Transactional(readOnly = true)
    public UserVerificationStatusResponse getStatus(UUID userId) {
        User user = userService.getById(userId);
        return toStatusResponse(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationDispatchResponse requestEmailOtp(UUID userId) {
        User user = userService.getById(userId);
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        String email = normalizeRequired(user.getEmail(), "Email");
        return issueOtp(user, VerificationPurpose.EMAIL, email);
    }

    public VerificationDispatchResponse requestPhoneOtp(UUID userId) {
        User user = userService.getById(userId);
        if (user.isPhoneVerified()) {
            throw new BadRequestException("Phone number is already verified");
        }

        String phone = normalizeRequired(user.getPhone(), "Phone number");
        return issueOtp(user, VerificationPurpose.PHONE, phone);
    }

    public UserVerificationStatusResponse verifyEmailOtp(UUID userId, String otp) {
        return verifyOtp(userId, VerificationPurpose.EMAIL, otp);
    }

    public UserVerificationStatusResponse verifyPhoneOtp(UUID userId, String otp) {
        return verifyOtp(userId, VerificationPurpose.PHONE, otp);
    }

    private VerificationDispatchResponse issueOtp(User user, VerificationPurpose purpose, String targetValue) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(otpExpiresMinutes);
        String otp = otpService.generate6Digits();

        VerificationOtpChallenge challenge = challengeRepository
                .findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseGet(() -> VerificationOtpChallenge.builder()
                        .userId(user.getId())
                        .purpose(purpose)
                        .build());

        challenge.setTargetValue(targetValue);
        challenge.setOtpCodeHash(otpService.hash(otp));
        challenge.setExpiresAt(expiresAt);
        challenge.setFailedAttempts(0);
        challenge.setLockedUntil(null);
        challenge.setConsumedAt(null);
        challenge.setLastSentAt(now);
        challengeRepository.save(challenge);

        if (purpose == VerificationPurpose.EMAIL) {
            notificationService.sendEmailOtp(user, targetValue, otp, expiresAt);
        } else {
            notificationService.sendPhoneOtp(user, targetValue, otp, expiresAt);
        }

        return VerificationDispatchResponse.builder()
                .purpose(purpose.name())
                .destination(maskTarget(targetValue, purpose))
                .expiresAt(expiresAt)
                .message("OTP sent for " + purpose.name().toLowerCase() + " verification")
                .build();
    }

    private UserVerificationStatusResponse verifyOtp(UUID userId, VerificationPurpose purpose, String rawOtp) {
        User user = userService.getById(userId);
        ensureNotAlreadyVerified(user, purpose);

        VerificationOtpChallenge challenge = challengeRepository
                .findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(userId, purpose)
                .orElseThrow(() -> new BadRequestException("No OTP request found. Request a new code first."));

        LocalDateTime now = LocalDateTime.now();
        if (challenge.getLockedUntil() != null && challenge.getLockedUntil().isAfter(now)) {
            throw new BadRequestException("OTP verification locked until " + challenge.getLockedUntil());
        }
        if (challenge.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("OTP expired. Request a new code.");
        }

        String currentTarget = resolveCurrentTarget(user, purpose);
        if (!challenge.getTargetValue().equals(currentTarget)) {
            throw new BadRequestException("Verification target changed. Request a new code.");
        }

        String otp = normalizeRequired(rawOtp, "OTP");
        if (!otpService.matches(otp, challenge.getOtpCodeHash())) {
            int attempts = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                challenge.setLockedUntil(now.plusMinutes(lockMinutes));
            }
            challengeRepository.save(challenge);
            throw new BadRequestException("Invalid OTP");
        }

        challenge.setFailedAttempts(0);
        challenge.setLockedUntil(null);
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);

        if (purpose == VerificationPurpose.EMAIL) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
        } else {
            user.setPhoneVerified(true);
            user.setPhoneVerifiedAt(now);
        }
        userRepository.save(user);

        return toStatusResponse(user);
    }

    private void ensureNotAlreadyVerified(User user, VerificationPurpose purpose) {
        if (purpose == VerificationPurpose.EMAIL && user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        if (purpose == VerificationPurpose.PHONE && user.isPhoneVerified()) {
            throw new BadRequestException("Phone number is already verified");
        }
    }

    private String resolveCurrentTarget(User user, VerificationPurpose purpose) {
        if (purpose == VerificationPurpose.EMAIL) {
            return normalizeRequired(user.getEmail(), "Email");
        }
        return normalizeRequired(user.getPhone(), "Phone number");
    }

    private UserVerificationStatusResponse toStatusResponse(User user) {
        return UserVerificationStatusResponse.builder()
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .build();
    }

    private String maskTarget(String targetValue, VerificationPurpose purpose) {
        if (purpose == VerificationPurpose.EMAIL) {
            int atIndex = targetValue.indexOf('@');
            if (atIndex <= 1) {
                return targetValue;
            }
            return targetValue.charAt(0) + "*".repeat(Math.max(1, atIndex - 1)) + targetValue.substring(atIndex);
        }

        if (targetValue.length() <= 4) {
            return targetValue;
        }
        return "*".repeat(targetValue.length() - 4) + targetValue.substring(targetValue.length() - 4);
    }

    private String normalizeRequired(String value, String fieldName) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return trimmed;
    }
}


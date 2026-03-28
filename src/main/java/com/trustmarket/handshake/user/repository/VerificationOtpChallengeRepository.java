package com.trustmarket.handshake.user.repository;

import com.trustmarket.handshake.user.entity.VerificationOtpChallenge;
import com.trustmarket.handshake.user.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationOtpChallengeRepository extends JpaRepository<VerificationOtpChallenge, UUID> {
    Optional<VerificationOtpChallenge> findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId, VerificationPurpose purpose);
}


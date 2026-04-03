package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustBasedEscrowService {

    @Value("${escrow.trust.thresholds.trusted-score:80}")
    private int trustedScoreThreshold;

    @Value("${escrow.trust.thresholds.verified-score:60}")
    private int verifiedScoreThreshold;

    @Value("${escrow.trust.thresholds.standard-score:40}")
    private int standardScoreThreshold;

    @Value("${escrow.trust.thresholds.limited-score:20}")
    private int limitedScoreThreshold;

    @Value("${escrow.trust.thresholds.high-value:5000}")
    private BigDecimal highValueThreshold;

    @Value("${escrow.trust.hold-hours.trusted:0}")
    private int trustedHoldHours;

    @Value("${escrow.trust.hold-hours.verified:24}")
    private int verifiedHoldHours;

    @Value("${escrow.trust.hold-hours.standard:48}")
    private int standardHoldHours;

    @Value("${escrow.trust.hold-hours.limited:168}")
    private int limitedHoldHours;

    @Value("${escrow.trust.hold-hours.restricted:336}")
    private int restrictedHoldHours;

    @Value("${escrow.trust.high-value-extra-hold-hours:24}")
    private int highValueExtraHoldHours;

    private final TransactionRepository transactionRepository;
    private final TrustScoreService trustScoreService;

    @Transactional
    public int applyHoldPolicy(Transaction txn) {
        User seller = txn.getSeller();
        int trustScore = trustScoreService.recalculateAndSave(seller);
        int holdHours = calculateHoldHours(trustScore, txn.getAmount());

        LocalDateTime now = LocalDateTime.now();
        if (txn.getEscrowHeldAt() == null) {
            txn.setEscrowHeldAt(now);
        }
        txn.setEscrowReleaseAt(holdHours > 0 ? now.plusHours(holdHours) : now);
        transactionRepository.save(txn);

        log.info(
                "Applied trust escrow hold for txn {} seller {} trustScore={} holdHours={}",
                txn.getId(), seller.getId(), trustScore, holdHours
        );

        return holdHours;
    }

    int calculateHoldHours(int trustScore, BigDecimal amount) {
        boolean highValue = amount != null && amount.compareTo(highValueThreshold) > 0;

        if (trustScore >= trustedScoreThreshold) {
            return trustedHoldHours + (highValue ? highValueExtraHoldHours : 0);
        }
        if (trustScore >= verifiedScoreThreshold) {
            return verifiedHoldHours + (highValue ? highValueExtraHoldHours : 0);
        }
        if (trustScore >= standardScoreThreshold) {
            return standardHoldHours + (highValue ? highValueExtraHoldHours : 0);
        }
        if (trustScore >= limitedScoreThreshold) {
            return limitedHoldHours;
        }
        return restrictedHoldHours;
    }
}


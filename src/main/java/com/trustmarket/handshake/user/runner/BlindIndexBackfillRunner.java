package com.trustmarket.handshake.user.runner;

import com.trustmarket.handshake.shared.security.HashUtil;
import com.trustmarket.handshake.user.entity.PayoutMethod;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.PayoutMethodRepository;
import com.trustmarket.handshake.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-shot backfill runner that populates email_hash and account_number_hash for existing rows.
 *
 * <p>Reads in batches to avoid loading the entire table into memory. Enable with:
 * {@code handshake.backfill.enabled=true}. Should be disabled (default) once the backfill
 * is complete, then this class should be deleted entirely before the next major release.
 */
@Component
@RequiredArgsConstructor
public class BlindIndexBackfillRunner {

    private static final Logger log = LoggerFactory.getLogger(BlindIndexBackfillRunner.class);
    private static final int BATCH_SIZE = 200;

    private final UserRepository userRepository;
    private final PayoutMethodRepository payoutMethodRepository;

    @Value("${handshake.backfill.enabled:false}")
    private boolean enabled;

    @PostConstruct
    @Transactional
    public void runOnce() {
        if (!enabled) {
            log.info("BlindIndexBackfillRunner disabled (handshake.backfill.enabled=false)");
            return;
        }
        try {
            backfillUsers();
            backfillPayoutMethods();
        } catch (Exception e) {
            log.warn("BlindIndexBackfillRunner encountered an error, skipping: {}", e.getMessage(), e);
        }
    }

    private void backfillUsers() {
        int page = 0;
        int updated = 0;
        Slice<User> slice;
        do {
            Pageable pageable = PageRequest.of(page++, BATCH_SIZE);
            slice = userRepository.findAll(pageable);
            for (User u : slice) {
                if (u.getEmail() != null && (u.getEmailHash() == null || u.getEmailHash().isBlank())) {
                    String norm = u.getEmail().trim().toLowerCase();
                    u.setEmailHash(HashUtil.sha256Hex(norm));
                    userRepository.save(u);
                    updated++;
                }
            }
        } while (slice.hasNext());
        log.info("BlindIndexBackfillRunner: backfilled email_hash for {} users", updated);
    }

    private void backfillPayoutMethods() {
        int page = 0;
        int updated = 0;
        Slice<PayoutMethod> slice;
        do {
            Pageable pageable = PageRequest.of(page++, BATCH_SIZE);
            slice = payoutMethodRepository.findAll(pageable);
            for (PayoutMethod pm : slice) {
                if (pm.getAccountNumber() != null && (pm.getAccountNumberHash() == null || pm.getAccountNumberHash().isBlank())) {
                    String normAcct = pm.getAccountNumber().replaceAll("\\D+", "");
                    pm.setAccountNumberHash(HashUtil.sha256Hex(normAcct));
                    payoutMethodRepository.save(pm);
                    updated++;
                }
            }
        } while (slice.hasNext());
        log.info("BlindIndexBackfillRunner: backfilled account_number_hash for {} payout methods", updated);
    }
}



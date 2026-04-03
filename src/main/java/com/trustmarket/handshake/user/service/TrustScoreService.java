package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;
    private static final int BASE_SCORE = 30;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public int recalculateAndSave(User user) {
        int score = calculate(user);
        user.setTrustScore(score);
        userRepository.save(user);
        return score;
    }

    @Transactional(readOnly = true)
    public int calculate(User user) {
        int score = BASE_SCORE;

        if (user.isEmailVerified()) {
            score += 10;
        }
        if (user.isPhoneVerified()) {
            score += 10;
        }

        long completedDeals = transactionRepository.countByUserAndStatus(user.getId(), TransactionStatus.COMPLETED);
        long disputedDeals = transactionRepository.countByUserAndStatus(user.getId(), TransactionStatus.DISPUTED)
                + transactionRepository.countByUserAndStatus(user.getId(), TransactionStatus.REFUNDED);

        score += Math.min((int) completedDeals, 30);
        score -= Math.min((int) disputedDeals * 10, 50);

        score += ratingBonus(user.getRatingAverage(), user.getRatingCount());

        long accountAgeDays = accountAgeDays(user);
        if (accountAgeDays < 7) {
            score = Math.min(score, 40);
        } else if (accountAgeDays >= 180) {
            score += 10;
        } else if (accountAgeDays >= 90) {
            score += 7;
        } else if (accountAgeDays >= 30) {
            score += 4;
        }

        BigDecimal completedValue = transactionRepository
                .sumAmountByUserAndStatus(user.getId(), TransactionStatus.COMPLETED);
        if (completedValue.compareTo(new BigDecimal("100000")) > 0) {
            score += 10;
        } else if (completedValue.compareTo(new BigDecimal("50000")) > 0) {
            score += 7;
        } else if (completedValue.compareTo(new BigDecimal("10000")) > 0) {
            score += 5;
        }

        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    private int ratingBonus(double avg, long count) {
        if (count < 5) {
            return 0;
        }
        if (avg >= 4.8d) {
            return 10;
        }
        if (avg >= 4.5d) {
            return 7;
        }
        if (avg >= 4.0d) {
            return 4;
        }
        return 0;
    }

    private long accountAgeDays(User user) {
        if (user.getCreatedAt() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now());
    }
}


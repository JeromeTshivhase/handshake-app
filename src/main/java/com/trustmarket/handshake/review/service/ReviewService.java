package com.trustmarket.handshake.review.service;

import com.trustmarket.handshake.review.dto.CreateReviewRequest;
import com.trustmarket.handshake.review.entity.Review;
import com.trustmarket.handshake.review.repository.ReviewRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TrustScoreService trustScoreService;

    @Transactional
    public Review create(UUID reviewerId, CreateReviewRequest req) {
        Transaction txn = transactionRepository.findById(req.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + req.getTransactionId()));

        if (txn.getStatus() != TransactionStatus.COMPLETED) {
            throw new BadRequestException("You can only review after the transaction is COMPLETED");
        }

        // Reviewer must be buyer or seller
        boolean reviewerIsBuyer = txn.getBuyer().getId().equals(reviewerId);
        boolean reviewerIsSeller = txn.getSeller().getId().equals(reviewerId);
        if (!reviewerIsBuyer && !reviewerIsSeller) {
            throw new ForbiddenException("You are not part of this transaction");
        }

        // Reviewee must be the other party
        UUID expectedReviewee = reviewerIsBuyer ? txn.getSeller().getId() : txn.getBuyer().getId();
        if (!expectedReviewee.equals(req.getRevieweeId())) {
            throw new BadRequestException("You can only review the other party in the transaction");
        }

        if (reviewRepository.existsByTransaction_IdAndReviewer_IdAndReviewee_Id(
                txn.getId(), reviewerId, req.getRevieweeId())) {
            throw new BadRequestException("You already reviewed this user for this transaction");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer not found: " + reviewerId));

        User reviewee = userRepository.findById(req.getRevieweeId())
                .orElseThrow(() -> new NotFoundException("Reviewee not found: " + req.getRevieweeId()));

        Review review = Review.builder()
                .transaction(txn)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        review = reviewRepository.save(review);

        // Update trust metrics (simple running average)
        long newCount = reviewee.getRatingCount() + 1;
        double newAvg = ((reviewee.getRatingAverage() * reviewee.getRatingCount()) + req.getRating()) / newCount;
        reviewee.setRatingCount(newCount);
        reviewee.setRatingAverage(newAvg);

        userRepository.save(reviewee);
        trustScoreService.recalculateAndSave(reviewee);

        return review;
    }

    @Transactional(readOnly = true)
    public Page<Review> listForUser(UUID revieweeId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return reviewRepository.findByReviewee_IdOrderByCreatedAtDesc(revieweeId, pageable);
    }
}
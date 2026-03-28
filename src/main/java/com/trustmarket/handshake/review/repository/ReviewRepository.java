package com.trustmarket.handshake.review.repository;

import com.trustmarket.handshake.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByReviewee_IdOrderByCreatedAtDesc(UUID revieweeId, Pageable pageable);
    boolean existsByTransaction_IdAndReviewer_IdAndReviewee_Id(UUID transactionId, UUID reviewerId, UUID revieweeId);
}
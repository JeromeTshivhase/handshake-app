package com.trustmarket.handshake.review.dto;

import com.trustmarket.handshake.review.entity.Review;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID transactionId;
    private UUID reviewerId;
    private UUID revieweeId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .transactionId(r.getTransaction().getId())
                .reviewerId(r.getReviewer().getId())
                .revieweeId(r.getReviewee().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
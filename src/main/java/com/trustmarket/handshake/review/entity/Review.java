package com.trustmarket.handshake.review.entity;

import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_txn_reviewer_reviewee",
                columnNames = {"transaction_id", "reviewer_id", "reviewee_id"}
        ),
        indexes = {
                @Index(name = "idx_review_reviewee", columnList = "reviewee_id"),
                @Index(name = "idx_review_txn", columnList = "transaction_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Column(nullable = false)
    private int rating; // 1..5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
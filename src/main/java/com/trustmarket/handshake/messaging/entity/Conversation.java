package com.trustmarket.handshake.messaging.entity;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversation_listing_buyer_seller",
                columnNames = {"listing_id", "buyer_id", "seller_id"}
        ),
        indexes = {
                @Index(name = "idx_convo_listing", columnList = "listing_id"),
                @Index(name = "idx_convo_buyer", columnList = "buyer_id"),
                @Index(name = "idx_convo_seller", columnList = "seller_id"),
                @Index(name = "idx_convo_last_message_at", columnList = "last_message_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "last_message_preview", columnDefinition = "TEXT")
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private long unreadCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean containsUser(UUID userId) {
        return buyer.getId().equals(userId) || seller.getId().equals(userId);
    }

    public UUID otherUser(UUID userId) {
        if (buyer.getId().equals(userId)) return seller.getId();
        if (seller.getId().equals(userId)) return buyer.getId();
        return null;
    }

    public void updateLastMessage(String content) {
        if (content == null) return;
        String trimmed = content.trim();
        this.lastMessagePreview = trimmed.length() > 100 ? trimmed.substring(0, 97) + "..." : trimmed;
        this.lastMessageAt = LocalDateTime.now();
    }
}
package com.trustmarket.handshake.messaging.entity;

import com.trustmarket.handshake.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_msg_convo", columnList = "conversation_id"),
        @Index(name = "idx_msg_created_at", columnList = "created_at"),
        @Index(name = "idx_msg_convo_visible", columnList = "conversation_id") // partial handled in queries
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "read_flag", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** Soft-delete: set when a participant deletes the message. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
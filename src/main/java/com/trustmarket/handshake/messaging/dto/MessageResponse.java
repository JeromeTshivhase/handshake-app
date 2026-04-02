package com.trustmarket.handshake.messaging.dto;

import com.trustmarket.handshake.messaging.entity.Message;
import com.trustmarket.handshake.messaging.entity.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private MessageType type;
    private String content;
    private boolean read;
    private boolean deleted;
    private LocalDateTime createdAt;

    public static MessageResponse fromEntity(Message m) {
        return fromEntity(m, null);
    }

    public static MessageResponse fromEntity(Message m, UUID viewerId) {
        boolean deleted = m.isDeleted();
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .type(m.getType())
                // Replace deleted content with placeholder for recipients; sender sees original.
                .content(deleted && (viewerId == null || !viewerId.equals(m.getSender().getId()))
                        ? "[Message deleted]"
                        : m.getContent())
                .read(m.isRead())
                .deleted(deleted)
                .createdAt(m.getCreatedAt())
                .build();
    }
}
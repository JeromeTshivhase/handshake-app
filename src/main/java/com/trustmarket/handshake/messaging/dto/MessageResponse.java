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
    private LocalDateTime createdAt;

    public static MessageResponse fromEntity(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .type(m.getType())
                .content(m.getContent())
                .read(m.isRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
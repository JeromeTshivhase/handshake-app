package com.trustmarket.handshake.messaging.ws.dto;

import com.trustmarket.handshake.messaging.entity.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsMessageEvent {
    private UUID conversationId;
    private UUID messageId;
    private UUID senderId;
    private MessageType type;
    private String content;
    private LocalDateTime createdAt;
}
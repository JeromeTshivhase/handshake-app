package com.trustmarket.handshake.messaging.ws.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record WsTypingIndicatorEvent(
        UUID conversationId,
        UUID userId,
        boolean typing,
        LocalDateTime at
) {
}


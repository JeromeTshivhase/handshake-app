package com.trustmarket.handshake.messaging.ws.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WsTypingIndicatorRequest {
    @NotNull
    private UUID conversationId;

    private boolean typing;
}


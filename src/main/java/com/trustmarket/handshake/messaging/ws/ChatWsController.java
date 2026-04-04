package com.trustmarket.handshake.messaging.ws;

import com.trustmarket.handshake.messaging.service.MessageService;
import com.trustmarket.handshake.messaging.service.TypingIndicatorService;
import com.trustmarket.handshake.messaging.ws.dto.WsTypingIndicatorRequest;
import com.trustmarket.handshake.messaging.ws.dto.WsSendMessageRequest;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final MessageService messageService;
    private final TypingIndicatorService typingIndicatorService;

    @Value("${messaging.max-content-length:5000}")
    private int maxContentLength;

    @MessageMapping("/chat.send")
    public void send(Authentication auth, WsSendMessageRequest req) {
        if (req == null) {
            throw new BadRequestException("Message payload is required");
        }
        if (req.getConversationId() == null) {
            throw new BadRequestException("conversationId is required");
        }
        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content is required");
        }
        if (req.getContent().length() > maxContentLength) {
            throw new BadRequestException("Message too long");
        }

        var senderId = AuthUtils.userId(auth);
        log.info("[{}] WS received message from user {} for conversation {}", Instant.now(), senderId, req.getConversationId());
        messageService.send(
                req.getConversationId(),
                senderId,
                req.getContent(),
                req.getType(),
                req.getChannels()
        );
    }

    @MessageMapping("/chat.typing")
    public void typing(Authentication auth, WsTypingIndicatorRequest req) {
        if (req == null || req.getConversationId() == null) {
            throw new BadRequestException("conversationId is required");
        }
        UUID senderId = AuthUtils.userId(auth);
        typingIndicatorService.publishTyping(req.getConversationId(), senderId, req.isTyping());
    }
}
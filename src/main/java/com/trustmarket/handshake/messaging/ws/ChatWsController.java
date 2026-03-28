package com.trustmarket.handshake.messaging.ws;

import com.trustmarket.handshake.messaging.service.MessageService;
import com.trustmarket.handshake.messaging.ws.dto.WsSendMessageRequest;
import com.trustmarket.handshake.shared.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void send(Authentication auth, WsSendMessageRequest req) {
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
}
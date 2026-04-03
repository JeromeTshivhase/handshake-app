package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.ws.dto.WsTypingIndicatorEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypingIndicatorService {

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public void publishTyping(UUID conversationId, UUID senderId, boolean typing) {
        var conversation = conversationService.getOwnedConversation(conversationId, senderId);
        UUID recipientId = conversation.otherUser(senderId);
        if (recipientId == null) {
            return;
        }

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/typing",
                WsTypingIndicatorEvent.builder()
                        .conversationId(conversationId)
                        .userId(senderId)
                        .typing(typing)
                        .at(LocalDateTime.now())
                        .build()
        );
    }
}


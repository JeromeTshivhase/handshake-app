package com.trustmarket.handshake.messaging.ws;

import com.trustmarket.handshake.messaging.service.MessageService;
import com.trustmarket.handshake.messaging.service.TypingIndicatorService;
import com.trustmarket.handshake.messaging.ws.dto.WsTypingIndicatorRequest;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWsControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private TypingIndicatorService typingIndicatorService;

    @Mock
    private Authentication authentication;

    private ChatWsController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatWsController(messageService, typingIndicatorService);
    }

    @Test
    void typingDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userId);

        WsTypingIndicatorRequest req = new WsTypingIndicatorRequest();
        req.setConversationId(conversationId);
        req.setTyping(true);

        controller.typing(authentication, req);

        verify(typingIndicatorService).publishTyping(conversationId, userId, true);
    }

    @Test
    void typingRejectsMissingConversationId() {
        WsTypingIndicatorRequest req = new WsTypingIndicatorRequest();

        assertThrows(BadRequestException.class, () -> controller.typing(authentication, req));
    }
}



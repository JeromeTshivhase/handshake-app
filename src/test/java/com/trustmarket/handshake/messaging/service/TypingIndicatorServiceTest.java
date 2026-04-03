package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TypingIndicatorServiceTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TypingIndicatorService service;

    @BeforeEach
    void setUp() {
        service = new TypingIndicatorService(conversationService, messagingTemplate);
    }

    @Test
    void publishTypingSendsEventToOtherParticipant() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User sender = User.builder().id(senderId).email("s@example.com").passwordHash("x").firstName("S").lastName("N").build();
        User recipient = User.builder().id(recipientId).email("r@example.com").passwordHash("x").firstName("R").lastName("C").build();
        Conversation conversation = Conversation.builder().id(conversationId).buyer(sender).seller(recipient).build();

        when(conversationService.getOwnedConversation(conversationId, senderId)).thenReturn(conversation);

        service.publishTyping(conversationId, senderId, true);

        verify(messagingTemplate).convertAndSendToUser(eq(recipientId.toString()), eq("/queue/typing"), any());
    }

    @Test
    void publishTypingDoesNotSendWhenAccessCheckFails() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        when(conversationService.getOwnedConversation(conversationId, senderId))
                .thenThrow(new BadRequestException("Not a participant"));

        assertThrows(BadRequestException.class, () -> service.publishTyping(conversationId, senderId, true));

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
}



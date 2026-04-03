package com.trustmarket.handshake.messaging.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuickReplyTemplateServiceTest {

    @Mock
    private ConversationService conversationService;

    private QuickReplyTemplateService service;

    @BeforeEach
    void setUp() {
        service = new QuickReplyTemplateService(conversationService);
    }

    @Test
    void listForConversationReturnsDefaultsAfterOwnershipCheck() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        var templates = service.listForConversation(conversationId, userId);

        verify(conversationService).getOwnedConversation(conversationId, userId);
        assertFalse(templates.isEmpty());
    }
}


package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.dto.QuickReplyTemplateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuickReplyTemplateService {

    private static final List<QuickReplyTemplateResponse> DEFAULT_TEMPLATES = List.of(
            QuickReplyTemplateResponse.builder().icon("💰").label("Best price?").message("What's your best price on this item?").build(),
            QuickReplyTemplateResponse.builder().icon("📍").label("Location?").message("Where are you located for pickup?").build(),
            QuickReplyTemplateResponse.builder().icon("📸").label("More photos?").message("Could you share more photos?").build(),
            QuickReplyTemplateResponse.builder().icon("✅").label("Condition?").message("Can you describe any defects or wear?").build(),
            QuickReplyTemplateResponse.builder().icon("🔒").label("Meetup?").message("When and where can we meet safely?").build()
    );

    private final ConversationService conversationService;

    public List<QuickReplyTemplateResponse> listForConversation(UUID conversationId, UUID userId) {
        // Access check: only participants may request quick replies.
        conversationService.getOwnedConversation(conversationId, userId);
        return DEFAULT_TEMPLATES;
    }
}


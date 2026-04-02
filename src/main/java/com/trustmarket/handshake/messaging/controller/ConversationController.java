package com.trustmarket.handshake.messaging.controller;

import com.trustmarket.handshake.messaging.dto.ConversationResponse;
import com.trustmarket.handshake.messaging.dto.InitiateConversationRequest;
import com.trustmarket.handshake.messaging.service.ConversationService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // Rule: must start from listing
    @PostMapping
    public ApiResponse<ConversationResponse> start(
            @PathVariable UUID listingId,
            Authentication auth,
            @Valid @RequestBody InitiateConversationRequest req) {

        UUID buyerId = AuthUtils.userId(auth);
        return ApiResponse.ok(conversationService.startFromListing(listingId, buyerId, req));
    }

}
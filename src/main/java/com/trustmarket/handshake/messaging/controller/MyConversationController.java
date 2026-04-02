package com.trustmarket.handshake.messaging.controller;

import com.trustmarket.handshake.messaging.dto.ConversationResponse;
import com.trustmarket.handshake.messaging.service.ConversationService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class MyConversationController {

    private final ConversationService conversationService;

    @GetMapping("/me")
    public ApiResponse<Page<ConversationResponse>> myConversations(Authentication auth,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "30") int pageSize) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(conversationService.myConversations(userId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getConversation(Authentication auth, @PathVariable UUID id) {
        UUID userId = AuthUtils.userId(auth);
        var conversation = conversationService.getOwnedConversation(id, userId);
        return ApiResponse.ok(ConversationResponse.fromEntity(conversation));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<Void> archive(Authentication auth, @PathVariable UUID id) {
        UUID userId = AuthUtils.userId(auth);
        conversationService.archive(id, userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/users/{userId}/block")
    public ApiResponse<Void> blockUser(Authentication auth, @PathVariable UUID userId) {
        UUID actorId = AuthUtils.userId(auth);
        conversationService.blockUser(actorId, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/users/{userId}/block")
    public ApiResponse<Void> unblockUser(Authentication auth, @PathVariable UUID userId) {
        UUID actorId = AuthUtils.userId(auth);
        conversationService.unblockUser(actorId, userId);
        return ApiResponse.ok(null);
    }
}

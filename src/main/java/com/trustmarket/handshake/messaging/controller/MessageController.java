package com.trustmarket.handshake.messaging.controller;

import com.trustmarket.handshake.messaging.dto.MessageResponse;
import com.trustmarket.handshake.messaging.dto.SendMessageRequest;
import com.trustmarket.handshake.messaging.service.MessageService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Send message", operationId = "sendMessage")
    public ApiResponse<MessageResponse> send(
            @PathVariable UUID conversationId,
            Authentication auth,
            @Valid @RequestBody SendMessageRequest req) {

        UUID senderId = AuthUtils.userId(auth);
        return ApiResponse.ok(messageService.send(
                conversationId,
                senderId,
                req.getContent(),
                req.getType(),
                req.getChannels()
        ));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List messages", operationId = "listMessages")
    public ApiResponse<Page<MessageResponse>> list(
            @PathVariable UUID conversationId,
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(messageService.list(conversationId, userId, page, pageSize));
    }

    @PostMapping("/mark-read")
    public ApiResponse<Integer> markAllAsRead(
            @PathVariable UUID conversationId,
            Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        int updated = messageService.markAllAsRead(conversationId, userId);
        return ApiResponse.ok(updated);
    }

    @PostMapping("/{messageId}/mark-read")
    public ApiResponse<Integer> markAsRead(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        int updated = messageService.markAsRead(conversationId, messageId, userId);
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete own message (soft delete)", operationId = "deleteMessage")
    public ApiResponse<Void> delete(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        messageService.delete(conversationId, messageId, userId);
        return ApiResponse.ok(null);
    }
}
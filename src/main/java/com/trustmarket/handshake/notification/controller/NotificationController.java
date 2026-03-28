package com.trustmarket.handshake.notification.controller;

import com.trustmarket.handshake.notification.dto.NotificationResponse;
import com.trustmarket.handshake.notification.service.NotificationService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get my notifications", operationId = "getMyNotifications")
    public ApiResponse<Page<NotificationResponse>> myNotifications(Authentication auth,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int pageSize) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(notificationService.listMyNotifications(userId, page, pageSize));
    }

    @PostMapping(value = "/{id}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mark notification as read", operationId = "markNotificationRead")
    public ApiResponse<NotificationResponse> markRead(Authentication auth, @PathVariable UUID id) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(notificationService.markRead(userId, id));
    }

    @GetMapping(value = "/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get unread notification count", operationId = "getUnreadNotificationCount")
    public ApiResponse<Integer> unreadCount(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        int count = notificationService.countUnread(userId);
        return ApiResponse.ok(count);
    }

    @PostMapping(value = "/mark-all-read", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mark all notifications as read", operationId = "markAllNotificationsRead")
    public ApiResponse<Void> markAllRead(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        notificationService.markAllRead(userId);
        return ApiResponse.ok(null);
    }
}

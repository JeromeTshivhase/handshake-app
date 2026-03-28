package com.trustmarket.handshake.notification.service;

import com.trustmarket.handshake.notification.entity.NotificationEventType;

import java.util.Map;
import java.util.UUID;

public record DomainNotificationEvent(
        UUID recipientUserId,
        NotificationEventType type,
        String title,
        String message,
        Map<String, Object> payload,
        boolean emailEligible
) {
}


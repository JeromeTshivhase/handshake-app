package com.trustmarket.handshake.notification.ws.dto;

import com.trustmarket.handshake.notification.entity.NotificationEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsNotificationEvent {
    private UUID id;
    private NotificationEventType type;
    private String title;
    private String message;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
}


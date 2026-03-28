package com.trustmarket.handshake.notification.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.entity.Notification;
import com.trustmarket.handshake.notification.entity.NotificationEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationEventType type;
    private String title;
    private String message;
    private Map<String, Object> payload;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification, ObjectMapper objectMapper) {
        Map<String, Object> payload = Collections.emptyMap();
        String payloadJson = notification.getPayloadJson();
        if (payloadJson != null && !payloadJson.isBlank()) {
            try {
                payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});
            } catch (Exception ignore) {
                payload = Collections.emptyMap();
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .payload(payload)
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}


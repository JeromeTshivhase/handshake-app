
package com.trustmarket.handshake.notification.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.dto.NotificationResponse;
import com.trustmarket.handshake.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class NotificationMapper {
    private final ObjectMapper objectMapper;

    public NotificationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationResponse toDto(Notification notification) {
        String payloadJson = notification.getPayloadJson();
        Map<String, Object> payload = Collections.emptyMap();
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



package com.trustmarket.handshake.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.entity.Notification;
import com.trustmarket.handshake.notification.ws.dto.WsNotificationEvent;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.UserSettings;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.notifications.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.notifications.email.from:${app.messaging.email.from:no-reply@handshake.local}}")
    private String fromAddress;

    public void deliver(Notification notification, boolean emailEligible) {
        deliverInApp(notification);
        if (emailEligible) {
            deliverEmail(notification);
        }
    }

    private void deliverInApp(Notification notification) {
        if (!isInAppEnabled(notification.getUserId())) {
            log.info("In-app notification delivery skipped for user {} due to user settings", notification.getUserId());
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(notification.getUserId().toString(), "/queue/notifications",
                    WsNotificationEvent.builder()
                            .id(notification.getId())
                            .type(notification.getType())
                            .title(notification.getTitle())
                            .message(notification.getMessage())
                            .payload(readPayload(notification.getPayloadJson()))
                            .createdAt(notification.getCreatedAt())
                            .build());
        } catch (Exception e) {
            log.error("Failed to deliver in-app notification {} to user {}: {}", notification.getId(), notification.getUserId(), e.getMessage(), e);
        }
    }

    private void deliverEmail(Notification notification) {
        if (!emailEnabled || !isEmailEnabled(notification.getUserId())) {
            log.info("Email notification delivery skipped for user {} due to settings or global disable", notification.getUserId());
            return;
        }

        User user = userRepository.findById(notification.getUserId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Notification email requested for user {} but no JavaMailSender is configured", notification.getUserId());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            mail.setFrom(fromAddress);
        }
        mail.setTo(user.getEmail());
        mail.setSubject(notification.getTitle());
        mail.setText(notification.getMessage());

        try {
            mailSender.send(mail);
        } catch (Exception e) {
            log.warn("Failed to send notification email {} to {}: {}", notification.getId(), user.getEmail(), e.getMessage());
        }
    }

    private boolean isInAppEnabled(UUID userId) {
        return userSettingsRepository.findById(userId)
                .map(UserSettings::isNotificationsEnabled)
                .orElse(true);
    }

    private boolean isEmailEnabled(UUID userId) {
        return userSettingsRepository.findById(userId)
                .map(settings -> settings.isNotificationsEnabled() && settings.isEmailNotifications())
                .orElse(true);
    }

    private Map<String, Object> readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // TODO: For distributed notification delivery at scale, consider using a message queue (e.g., RabbitMQ, Kafka)
    // and a dedicated notification worker service to process and deliver notifications asynchronously.
    // Current implementation is synchronous and may not scale for high throughput.
    // See NotificationEventListener and NotificationDeliveryService for event-driven delivery.
}

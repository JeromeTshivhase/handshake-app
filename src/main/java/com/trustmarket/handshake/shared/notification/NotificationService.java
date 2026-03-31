package com.trustmarket.handshake.shared.notification;

import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("userNotificationService")
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationPublisher publisher;

    public void sendUserCreated(User user) {
        try {
            publisher.publishUserCreated(user);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                .warn("Failed to publish user created notification: {}", e.getMessage());
        }
    }

    public void sendPasswordReset(User user, String token) {
        try {
            publisher.publishPasswordReset(user, token);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                .warn("Failed to publish password reset notification: {}", e.getMessage());
        }
    }
}



package com.trustmarket.handshake.shared.notification;

import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.notification.repository.NotificationRepository;
import com.trustmarket.handshake.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("userNotificationService")
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationPublisher publisher;
    private final NotificationRepository notificationRepository;

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

    /**
     * Backfill a welcome notification for legacy users who existed before notifications were enabled.
     */
    public void sendWelcomeIfNoNotifications(User user) {
        try {
            if (notificationRepository.countByUserId(user.getId()) == 0L) {
                publisher.publishUserCreated(user);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .warn("Failed to backfill welcome notification: {}", e.getMessage());
        }
    }
}



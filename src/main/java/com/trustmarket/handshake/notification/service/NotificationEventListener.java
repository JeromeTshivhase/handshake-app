
package com.trustmarket.handshake.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.entity.Notification;
import com.trustmarket.handshake.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryService notificationDeliveryService;
    private final ObjectMapper objectMapper;


    /**
     * Runs asynchronously (off the HTTP thread) in the dedicated notification
     * thread pool AFTER the publishing transaction has successfully committed.
     *
     * REQUIRES_NEW ensures this method runs in its own fresh transaction so
     * that the DB save is independent of the original business transaction.
     */
    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(DomainNotificationEvent event) {
        log.info("[NotificationEventListener] Received event: {} for user {}", event.type(), event.recipientUserId());
        Notification notification = Notification.builder()
                .userId(event.recipientUserId())
                .type(event.type())
                .title(event.title())
                .message(event.message())
                .payloadJson(writePayload(event))
                .build();
        try {
            notification = notificationRepository.save(notification);
            log.info("[NotificationEventListener] Notification saved: {}", notification.getId());
        } catch (Exception e) {
            log.error("[NotificationEventListener] Failed to save notification for user {}: {}", event.recipientUserId(), e.getMessage(), e);
            return;
        }
        try {
            notificationDeliveryService.deliver(notification, event.emailEligible());
            log.info("[NotificationEventListener] Notification delivered (in-app/email): {}", notification.getId());
        } catch (Exception e) {
            log.error("[NotificationEventListener] Failed to deliver notification {}: {}", notification.getId(), e.getMessage(), e);
        }
    }

    private String writePayload(DomainNotificationEvent event) {
        try {
            if (event.payload() == null || event.payload().isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(event.payload());
        } catch (Exception e) {
            log.error("[NotificationEventListener] Failed to serialize payload for event {}: {}", event.type(), e.getMessage(), e);
            return null;
        }
    }
}


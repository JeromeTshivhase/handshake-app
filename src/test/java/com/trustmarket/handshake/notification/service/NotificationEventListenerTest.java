package com.trustmarket.handshake.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.entity.Notification;
import com.trustmarket.handshake.notification.entity.NotificationEventType;
import com.trustmarket.handshake.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    private NotificationEventListener notificationEventListener;

    @BeforeEach
    void setUp() {
        notificationEventListener = new NotificationEventListener(notificationRepository, notificationDeliveryService, new ObjectMapper());
    }

    @Test
    void onNotificationPersistsAndDeliversNotification() {
        DomainNotificationEvent event = new DomainNotificationEvent(
                UUID.randomUUID(),
                NotificationEventType.USER_CREATED,
                "Welcome",
                "Account created",
                Map.of("userId", "123"),
                true
        );

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationEventListener.onNotification(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationDeliveryService).deliver(any(Notification.class), org.mockito.Mockito.eq(true));
    }
}


package com.trustmarket.handshake.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.dto.NotificationResponse;
import com.trustmarket.handshake.notification.entity.Notification;
import com.trustmarket.handshake.notification.repository.NotificationRepository;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listMyNotifications(UUID userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notification -> NotificationResponse.fromEntity(notification, objectMapper));
    }

    @Transactional(readOnly = true)
    public int countUnread(UUID userId) {
        return (int) notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only update your own notifications");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification, objectMapper);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        // Use a bulk update query for efficiency
        notificationRepository.updateReadByUserId(userId);
    }
}

package com.trustmarket.handshake.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.dto.NotificationResponse;
import com.trustmarket.handshake.notification.entity.Notification;
import com.trustmarket.handshake.notification.mapper.NotificationMapper;
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
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public int countUnread(UUID userId) {
        return (int) notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notificationMapper.toDto(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        // Use a bulk update query for efficiency
        notificationRepository.updateReadByUserId(userId);
    }

    @Transactional
    public void deleteBulk(UUID userId, java.util.List<UUID> ids) {
        // Only delete notifications belonging to the user
        var notifications = notificationRepository.findAllById(ids);
        for (var n : notifications) {
            if (n.getUserId().equals(userId)) {
                notificationRepository.delete(n);
            }
        }
    }
}

package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.dto.MessageResponse;
import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.entity.ConversationStatus;
import com.trustmarket.handshake.messaging.entity.Message;
import com.trustmarket.handshake.messaging.entity.MessageDeliveryChannel;
import com.trustmarket.handshake.messaging.entity.MessageType;
import com.trustmarket.handshake.messaging.repository.ConversationRepository;
import com.trustmarket.handshake.messaging.repository.MessageRepository;
import com.trustmarket.handshake.messaging.repository.UserBlockRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.shared.rate.RateLimiterService;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimiterService rateLimiterService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageSafetyService messageSafetyService;

    @Value("${messaging.rate-limit.messages-per-minute:30}")
    private int messagesPerMinute;

    @Value("${messaging.preview-length:100}")
    private int previewLength;

    @Value("${messaging.max-content-length:5000}")
    private int maxContentLength;

    @Value("${messaging.safety.mode:warn}")
    private String safetyMode;

    @Value("${messaging.safety.sanitize-content:true}")
    private boolean sanitizeContent;

    @Value("${messaging.safety.block-social-handles:false}")
    private boolean blockSocialHandles;

    @Value("${messaging.safety.block-phone-numbers:false}")
    private boolean blockPhoneNumbers;

    @Transactional
    public MessageResponse send(UUID conversationId, UUID senderId, String content) {
        return send(conversationId, senderId, content, MessageType.TEXT, EnumSet.of(MessageDeliveryChannel.IN_APP));
    }

    @Transactional
    public MessageResponse send(
            UUID conversationId,
            UUID senderId,
            String content,
            MessageType type,
            Set<MessageDeliveryChannel> channels
    ) {
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new BadRequestException("Message content is required");
        }
        if (content.length() > maxContentLength) {
            throw new BadRequestException("Message too long (max " + maxContentLength + " characters)");
        }

        // Rate limit per sender
        var bucket = rateLimiterService.resolveBucket(
                "msg:" + senderId, messagesPerMinute, Duration.ofMinutes(1));
        if (!bucket.tryConsume(1)) {
            throw new BadRequestException("Rate limit exceeded. Please slow down.");
        }

        log.info("[{}] MessageService.send START for conversation {} by user {}", Instant.now(), conversationId, senderId);

        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));

        if (!c.containsUser(senderId)) {
            throw new BadRequestException("Not a participant of this conversation");
        }

        if (c.getStatus() == ConversationStatus.BLOCKED) {
            throw new BadRequestException("This conversation is blocked");
        }

        UUID otherUserId = c.otherUser(senderId);
        if (otherUserId != null && (userBlockRepository.existsByBlocker_IdAndBlocked_Id(senderId, otherUserId)
                || userBlockRepository.existsByBlocker_IdAndBlocked_Id(otherUserId, senderId))) {
            throw new BadRequestException("Messaging is blocked between these users");
        }

        String normalized = content.trim();
        MessageSafetyService.MessageSafetyAssessment safety = messageSafetyService.assess(normalized);
        String effectiveContent = sanitizeContent ? safety.sanitizedContent() : normalized;

        if ("block".equalsIgnoreCase(safetyMode) && shouldBlock(safety)) {
            throw new BadRequestException("Message blocked for safety reasons. Keep communication on-platform.");
        }
        if (!safety.flags().isEmpty()) {
            log.warn("Message safety flags for user {} conversation {}: {}", senderId, conversationId, safety.flags());
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException("Sender not found: " + senderId));

        Message msg = Message.builder()
                .conversation(c)
                .sender(sender)
                .type(type == null ? MessageType.TEXT : type)
                .content(effectiveContent)
                .read(false)
                .build();

        msg = messageRepository.save(msg);
        log.info("[{}] MessageService.send saved message {}", Instant.now(), msg.getId());

        // Atomic conversation metadata update
        c.updateLastMessage(msg.getContent(), previewLength);
        conversationRepository.save(c);

        // Atomic unread increment — avoids race condition
        if (otherUserId != null) {
            conversationRepository.incrementUnreadCount(c.getId());
        }

        eventPublisher.publishEvent(new MessageCreatedEvent(
                msg.getId(),
                normalizeChannels(channels)
        ));

        return MessageResponse.fromEntity(msg);
    }

    private boolean shouldBlock(MessageSafetyService.MessageSafetyAssessment safety) {
        if (safety.highRisk()) {
            return true;
        }
        if (blockSocialHandles && safety.hasFlag("SOCIAL_MEDIA_HANDLE")) {
            return true;
        }
        return blockPhoneNumbers && safety.hasFlag("PHONE_NUMBER");
    }

    private Set<MessageDeliveryChannel> normalizeChannels(Set<MessageDeliveryChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return EnumSet.of(MessageDeliveryChannel.IN_APP);
        }
        return EnumSet.copyOf(channels);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> list(UUID conversationId, UUID userId, int page, int pageSize) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!c.containsUser(userId)) throw new BadRequestException("Not a participant");

        Pageable pageable = PageRequest.of(page, pageSize);
        return messageRepository.findVisibleByConversationId(conversationId, pageable)
                .map(m -> MessageResponse.fromEntity(m, userId));
    }

    @Transactional
    public int markAllAsRead(UUID conversationId, UUID userId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!c.containsUser(userId)) {
            throw new ForbiddenException("Not a participant of this conversation");
        }

        int updated = messageRepository.markAllAsReadByConversationAndUser(conversationId, userId);
        if (updated > 0) {
            c.setUnreadCount(0);
            conversationRepository.save(c);

            // Notify the sender(s) that messages were read
            UUID otherUser = c.otherUser(userId);
            if (otherUser != null) {
                messagingTemplate.convertAndSendToUser(
                        otherUser.toString(),
                        "/queue/read-receipts",
                        Map.of("conversationId", conversationId, "readBy", userId));
            }
        }
        return updated;
    }

    @Transactional
    public int markAsRead(UUID conversationId, UUID messageId, UUID userId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!c.containsUser(userId)) {
            throw new ForbiddenException("Not a participant of this conversation");
        }

        int updated = messageRepository.markAsReadByIdAndUser(conversationId, messageId, userId);
        if (updated > 0) {
            // Efficient count instead of load-all
            long unread = messageRepository.countUnread(conversationId, userId);
            c.setUnreadCount(unread);
            conversationRepository.save(c);

            // Send read-receipt WebSocket event to the message sender
            UUID otherUser = c.otherUser(userId);
            if (otherUser != null) {
                messagingTemplate.convertAndSendToUser(
                        otherUser.toString(),
                        "/queue/read-receipts",
                        Map.of("conversationId", conversationId, "messageId", messageId, "readBy", userId));
            }
        }
        return updated;
    }

    @Transactional
    public void delete(UUID conversationId, UUID messageId, UUID userId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!c.containsUser(userId)) {
            throw new ForbiddenException("Not a participant of this conversation");
        }

        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));

        if (!msg.getConversation().getId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        if (!msg.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own messages");
        }

        if (msg.isDeleted()) {
            return; // idempotent
        }

        msg.setDeletedAt(LocalDateTime.now());
        msg.setDeletedBy(userId);
        messageRepository.save(msg);

        log.info("Message {} soft-deleted by user {}", messageId, userId);
    }
}



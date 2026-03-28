package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.dto.MessageResponse;
import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.entity.MessageDeliveryChannel;
import com.trustmarket.handshake.messaging.entity.Message;
import com.trustmarket.handshake.messaging.entity.MessageType;
import com.trustmarket.handshake.messaging.repository.ConversationRepository;
import com.trustmarket.handshake.messaging.repository.MessageRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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
        log.info("[{}] MessageService.send START for conversation {} by user {}", Instant.now(), conversationId, senderId);
        if (content == null || content.trim().isEmpty()) {
            throw new BadRequestException("Message content is required");
        }

        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));

        if (!c.containsUser(senderId)) {
            throw new BadRequestException("Not a participant of this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException("Sender not found: " + senderId));

        Message msg = Message.builder()
                .conversation(c)
                .sender(sender)
                .type(type == null ? MessageType.TEXT : type)
                .content(content.trim())
                .read(false)
                .build();

        log.info("[{}] MessageService.send about to save message to DB", Instant.now());
        msg = messageRepository.save(msg);
        log.info("[{}] MessageService.send finished saving message {} to DB", Instant.now(), msg.getId());

        // update conversation preview/unread
        c.updateLastMessage(msg.getContent());
        UUID other = c.otherUser(senderId);
        if (other != null) {
            c.setUnreadCount(c.getUnreadCount() + 1);
        }
        conversationRepository.save(c);

        eventPublisher.publishEvent(new MessageCreatedEvent(
                msg.getId(),
                normalizeChannels(channels)
        ));

        return MessageResponse.fromEntity(msg);
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
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId, pageable)
                .map(MessageResponse::fromEntity);
    }
}
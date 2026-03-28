package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.entity.Message;
import com.trustmarket.handshake.messaging.entity.MessageDeliveryChannel;
import com.trustmarket.handshake.messaging.ws.dto.WsMessageEvent;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.UserSettings;
import com.trustmarket.handshake.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeliveryService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSettingsRepository userSettingsRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.messaging.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.messaging.email.from:no-reply@handshake.local}")
    private String fromAddress;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public void deliver(Message message, Set<MessageDeliveryChannel> requestedChannels) {
        Set<MessageDeliveryChannel> channels = normalizeChannels(requestedChannels);
        if (channels.isEmpty()) {
            return;
        }

        Conversation conversation = message.getConversation();
        User sender = message.getSender();
        User recipient = resolveRecipient(conversation, sender.getId());

        if (channels.contains(MessageDeliveryChannel.IN_APP)) {
            deliverInApp(message, sender.getId(), recipient.getId());
        }

        if (channels.contains(MessageDeliveryChannel.EMAIL)) {
            deliverEmail(message, sender, recipient);
        }
    }

    private void deliverInApp(Message message, UUID senderId, UUID recipientId) {
        log.info("[{}] MessageDeliveryService.deliverInApp about to deliver message {} to sender {} and recipient {}", Instant.now(), message.getId(), senderId, recipientId);
        WsMessageEvent event = WsMessageEvent.builder()
                .conversationId(message.getConversation().getId())
                .messageId(message.getId())
                .senderId(senderId)
                .type(message.getType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();

        messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", event);

        if (!senderId.equals(recipientId) && isInAppEnabled(recipientId)) {
            messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/messages", event);
        }
    }

    private void deliverEmail(Message message, User sender, User recipient) {
        if (!emailEnabled || !isEmailEnabled(recipient.getId())) {
            return;
        }

        String to = recipient.getEmail();
        if (to == null || to.isBlank()) {
            log.warn("Skipping message email for user {} because no recipient email is available", recipient.getId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("EMAIL channel requested for message {} but no JavaMailSender is configured", message.getId());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            mail.setFrom(fromAddress);
        }
        mail.setTo(to);
        mail.setSubject("New message about " + message.getConversation().getListing().getTitle());
        mail.setText(buildEmailBody(message, sender, recipient));

        try {
            mailSender.send(mail);
        } catch (Exception e) {
            log.warn("Failed to send message email for message {} to {}: {}", message.getId(), to, e.getMessage());
        }
    }

    private String buildEmailBody(Message message, User sender, User recipient) {
        String senderName = sender.fullName().trim().isEmpty() ? sender.getEmail() : sender.fullName().trim();
        String recipientName = recipient.fullName().trim().isEmpty() ? recipient.getEmail() : recipient.fullName().trim();
        String conversationLink = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? ""
                : "\nOpen the conversation: " + frontendBaseUrl.replaceAll("/$", "") + "/messages/" + message.getConversation().getId();

        return "Hello " + recipientName + ",\n\n"
                + "You received a new message from " + senderName
                + " about \"" + message.getConversation().getListing().getTitle() + "\".\n\n"
                + "Message:\n"
                + message.getContent()
                + "\n\nConversation ID: " + message.getConversation().getId()
                + conversationLink
                + "\n\n- Handshake";
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

    private User resolveRecipient(Conversation conversation, UUID senderId) {
        if (conversation.getBuyer().getId().equals(senderId)) {
            return conversation.getSeller();
        }
        return conversation.getBuyer();
    }

    private Set<MessageDeliveryChannel> normalizeChannels(Set<MessageDeliveryChannel> requestedChannels) {
        if (requestedChannels == null || requestedChannels.isEmpty()) {
            return EnumSet.of(MessageDeliveryChannel.IN_APP);
        }
        return EnumSet.copyOf(requestedChannels);
    }
}

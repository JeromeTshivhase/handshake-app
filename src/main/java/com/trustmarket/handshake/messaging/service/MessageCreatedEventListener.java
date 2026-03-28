package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.entity.Message;
import com.trustmarket.handshake.messaging.repository.MessageRepository;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MessageCreatedEventListener {

    private final MessageRepository messageRepository;
    private final MessageDeliveryService messageDeliveryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreatedEvent event) {
        Message message = messageRepository.findDetailedById(event.messageId())
                .orElseThrow(() -> new NotFoundException("Message not found: " + event.messageId()));

        messageDeliveryService.deliver(message, event.channels());
    }
}


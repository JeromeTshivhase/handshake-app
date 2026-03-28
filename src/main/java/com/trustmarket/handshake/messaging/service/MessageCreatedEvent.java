package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.messaging.entity.MessageDeliveryChannel;

import java.util.Set;
import java.util.UUID;

public record MessageCreatedEvent(UUID messageId, Set<MessageDeliveryChannel> channels) {
}


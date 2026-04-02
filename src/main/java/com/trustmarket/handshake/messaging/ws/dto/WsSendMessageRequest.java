package com.trustmarket.handshake.messaging.ws.dto;

import com.trustmarket.handshake.messaging.entity.MessageDeliveryChannel;
import com.trustmarket.handshake.messaging.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Data
public class WsSendMessageRequest {
    @NotNull
    private UUID conversationId;

    @NotBlank
    @Size(max = 5000)
    private String content;

    private MessageType type = MessageType.TEXT;
    private Set<MessageDeliveryChannel> channels = EnumSet.of(MessageDeliveryChannel.IN_APP);
}
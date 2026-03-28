package com.trustmarket.handshake.messaging.dto;

import com.trustmarket.handshake.messaging.entity.MessageDeliveryChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.EnumSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateConversationRequest {
    @NotBlank
    @Size(max = 5000)
    private String initialMessage;

    @Builder.Default
    private Set<MessageDeliveryChannel> channels = EnumSet.of(MessageDeliveryChannel.IN_APP);
}
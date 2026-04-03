package com.trustmarket.handshake.messaging.dto;

import lombok.Builder;

@Builder
public record QuickReplyTemplateResponse(
        String label,
        String message,
        String icon
) {
}


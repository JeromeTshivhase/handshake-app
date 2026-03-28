package com.trustmarket.handshake.messaging.dto;

import com.trustmarket.handshake.messaging.entity.Conversation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private UUID id;
    private UUID listingId;
    private String listingTitle;
    private UUID buyerId;
    private String buyerName;
    private String buyerAvatar;
    private Boolean buyerVerified;
    private UUID sellerId;
    private String sellerName;
    private String sellerAvatar;
    private Boolean sellerVerified;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private LocalDateTime createdAt;

    public static ConversationResponse fromEntity(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .listingId(c.getListing().getId())
                .listingTitle(c.getListing().getTitle())
                .buyerId(c.getBuyer().getId())
                .buyerName(c.getBuyer().fullName())
                .buyerAvatar(c.getBuyer().getProfilePictureUrl()) // fixed: was getAvatarUrl()
                .buyerVerified(c.getBuyer().isVerified())
                .sellerId(c.getSeller().getId())
                .sellerName(c.getSeller().fullName())
                .sellerAvatar(c.getSeller().getProfilePictureUrl()) // fixed: was getAvatarUrl()
                .sellerVerified(c.getSeller().isVerified())
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(c.getUnreadCount())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
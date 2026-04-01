package com.trustmarket.handshake.listing.dto;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingCondition;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {

    private UUID id;
    private String title;
    private String description;

    private BigDecimal price;
    private String currency;
    private ListingCondition condition;

    private UUID categoryId;
    private String categoryName;

    private UUID sellerId;
    private String sellerName;
    private String sellerAvatar;
    private boolean sellerVerified;
    private int sellerTrustScore;
    private double sellerRating;
    private long sellerReviewCount;

    private ListingStatus status;
    private String location;

    private List<String> imageUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ListingResponse fromEntity(Listing l, String categoryName, List<String> images, String sellerAvatar, boolean sellerVerified, int sellerTrustScore, double sellerRating, long sellerReviewCount) {
        return ListingResponse.builder()
                .id(l.getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .price(l.getPrice())
                .currency(l.getCurrency().name())
                .condition(l.getCondition())
                .categoryId(l.getCategory() == null ? null : l.getCategory().getId())
                .categoryName(categoryName)
                .sellerId(l.getSeller().getId())
                .sellerName(l.getSeller().fullName())
                .sellerAvatar(sellerAvatar)
                .sellerVerified(sellerVerified)
                .sellerTrustScore(sellerTrustScore)
                .sellerRating(sellerRating)
                .sellerReviewCount(sellerReviewCount)
                .status(l.getStatus())
                .location(l.getLocation())
                .imageUrls(images == null ? List.of() : images)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
package com.trustmarket.handshake.listing.dto;

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

    private List<ListingImageDto> images;

    private List<String> imageUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
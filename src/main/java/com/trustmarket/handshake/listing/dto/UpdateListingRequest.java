package com.trustmarket.handshake.listing.dto;

import com.trustmarket.handshake.listing.entity.ListingCondition;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    private BigDecimal price;

    private ListingCondition condition;

    private UUID categoryId;

    @Size(max = 255)
    private String location;

    private ListingStatus status; // allow seller to set INACTIVE etc.

    private List<@Size(max = 2000) String> imageUrls; // optional replacement
}
package com.trustmarket.handshake.listing.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchListingsRequest {
    private String keyword;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String location;
    private String sortBy; // "newest", "price_asc", "price_desc"

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int pageSize = 20;
}
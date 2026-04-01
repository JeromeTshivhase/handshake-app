package com.trustmarket.handshake.listing.dto;

import com.trustmarket.handshake.listing.entity.ListingCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateListingRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    private ListingCondition condition;

    private UUID categoryId;

    @Size(max = 255)
    private String location;

    private List<@Valid ListingImageInput> images;

    private List<@Size(max = 2000) String> imageUrls;
}
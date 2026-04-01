package com.trustmarket.handshake.listing.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImageDto {
    private UUID id;
    private String url;
    private String publicId;
    private Integer position;
}


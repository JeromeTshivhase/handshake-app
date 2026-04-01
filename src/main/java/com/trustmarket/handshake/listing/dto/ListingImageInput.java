package com.trustmarket.handshake.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImageInput {

    @NotBlank
    @Size(max = 2000)
    private String url;

    @NotBlank
    @Size(max = 255)
    private String publicId;
}


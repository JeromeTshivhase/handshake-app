package com.trustmarket.handshake.safe_meetup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoPointRequest {
    @NotNull private Double lat;
    @NotNull private Double lng;
}
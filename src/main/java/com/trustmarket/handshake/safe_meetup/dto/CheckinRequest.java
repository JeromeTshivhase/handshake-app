package com.trustmarket.handshake.safe_meetup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckinRequest {
    @NotNull
    private Double lat;

    @NotNull
    private Double lng;
}


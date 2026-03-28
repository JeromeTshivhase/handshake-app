package com.trustmarket.handshake.safe_meetup.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpEnableResponse {
    private boolean enabled;
    private double distanceMeters;
    private boolean withinTimeWindow;
    private List<String> reasons; // e.g. ["MEETUP_NOT_SCHEDULED","TOO_FAR"]
}
package com.trustmarket.handshake.safe_meetup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetupRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @Size(max = 255)
    private String addressLabel;

    @NotNull
    private LocalDateTime scheduledTime;
}
package com.trustmarket.handshake.safe_meetup.dto;

import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetupResponse {
    private UUID id;
    private UUID handshakeId;
    private double latitude;
    private double longitude;
    private String addressLabel;
    private LocalDateTime scheduledTime;
    private MeetupStatus status;

    public static MeetupResponse from(Meetup m) {
        return MeetupResponse.builder()
                .id(m.getId())
                .handshakeId(m.getHandshake().getId())
                .latitude(m.getLatitude())
                .longitude(m.getLongitude())
                .addressLabel(m.getAddressLabel())
                .scheduledTime(m.getScheduledTime())
                .status(m.getStatus())
                .build();
    }
}
package com.trustmarket.handshake.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDto {
    private UUID userId;
    private String bio;
    private String location;
    private String city;
    private String province;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



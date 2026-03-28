package com.trustmarket.handshake.user.dto;

import com.trustmarket.handshake.user.entity.UserRole;
import com.trustmarket.handshake.user.entity.UserStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;

    private String firstName;
    private String lastName;
    private String phone;
    private String profilePictureUrl;

    private UserStatus status;
    private UserRole role;

    private boolean emailVerified;
    private LocalDateTime emailVerifiedAt;

    private boolean phoneVerified;
    private LocalDateTime phoneVerifiedAt;

    private LocalDateTime lastLoginAt;

    private double ratingAverage;
    private long ratingCount;
    private int trustScore;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String bio;
    private String city;
    private String province;
    private boolean verified;
}
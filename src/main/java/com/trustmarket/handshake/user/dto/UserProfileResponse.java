package com.trustmarket.handshake.user.dto;

import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private LocalDateTime createdAt;
    private String profilePictureUrl;
    private boolean verified;
    private double ratingAverage;
    private long ratingCount;
    private int trustScore;
    private String bio;
    private String city;
    private String province;

    public static UserProfileResponse fromEntity(User u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .createdAt(u.getCreatedAt())
                .profilePictureUrl(u.getProfilePictureUrl())
                .verified(u.isVerified())
                .ratingAverage(u.getRatingAverage())
                .ratingCount(u.getRatingCount())
                .trustScore(u.getTrustScore())
                .bio(u.getBio())
                .city(u.getCity())
                .province(u.getProvince())
                .build();
    }
}
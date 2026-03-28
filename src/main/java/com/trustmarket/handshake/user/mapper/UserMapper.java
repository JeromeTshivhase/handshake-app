package com.trustmarket.handshake.user.mapper;

import com.trustmarket.handshake.user.dto.UserProfileDto;
import com.trustmarket.handshake.user.dto.UserResponse;
import com.trustmarket.handshake.user.dto.UserSettingsDto;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.UserProfile;
import com.trustmarket.handshake.user.entity.UserSettings;

public class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .phone(u.getPhone())
                .profilePictureUrl(u.getProfilePictureUrl())
                .status(u.getStatus())
                .role(u.getRole())
                .emailVerified(u.isEmailVerified())
                .emailVerifiedAt(u.getEmailVerifiedAt())
                .phoneVerified(u.isPhoneVerified())
                .phoneVerifiedAt(u.getPhoneVerifiedAt())
                .lastLoginAt(u.getLastLoginAt())
                .ratingAverage(u.getRatingAverage())
                .ratingCount(u.getRatingCount())
                .trustScore(u.getTrustScore())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .bio(u.getBio())
                .city(u.getCity())
                .province(u.getProvince())
                .verified(u.isVerified())
                .build();
    }

    public static UserProfileDto toProfileDto(UserProfile p) {
        return UserProfileDto.builder()
                .userId(p.getUserId())
                .bio(p.getBio())
                .location(p.getLocation())
                .city(p.getCity())
                .province(p.getProvince())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static UserSettingsDto toSettingsDto(UserSettings s) {
        return UserSettingsDto.builder()
                .userId(s.getUserId())
                .notificationsEnabled(s.isNotificationsEnabled())
                .emailNotifications(s.isEmailNotifications())
                .darkMode(s.isDarkMode())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
package com.trustmarket.handshake.user.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.user.dto.UserProfileDto;
import com.trustmarket.handshake.user.dto.UserSettingsDto;
import com.trustmarket.handshake.user.dto.UpdateProfileRequest;
import com.trustmarket.handshake.user.dto.UpdateSettingsRequest;
import com.trustmarket.handshake.user.mapper.UserMapper;
import com.trustmarket.handshake.user.service.UserProfileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public ApiResponse<UserProfileDto> myProfile(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(UserMapper.toProfileDto(userProfileService.getOrCreateProfile(userId)));
    }

    @PatchMapping("/profile")
    public ApiResponse<UserProfileDto> updateProfile(Authentication auth, @RequestBody UpdateProfileRequest req) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(UserMapper.toProfileDto(
                userProfileService.updateProfile(userId, req.getBio(), req.getLocation(), req.getCity(), req.getProvince())
        ));
    }

    @GetMapping("/settings")
    public ApiResponse<UserSettingsDto> mySettings(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(UserMapper.toSettingsDto(userProfileService.getOrCreateSettings(userId)));
    }

    @PatchMapping("/settings")
    public ApiResponse<UserSettingsDto> updateSettings(Authentication auth, @RequestBody UpdateSettingsRequest req) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(UserMapper.toSettingsDto(
                userProfileService.updateSettings(userId, req.getNotificationsEnabled(), req.getEmailNotifications(), req.getDarkMode())
        ));
    }

    // ...existing code...
}
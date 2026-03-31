package com.trustmarket.handshake.user.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.user.dto.UpdateUserRequest;
import com.trustmarket.handshake.user.dto.UserResponse;
import com.trustmarket.handshake.user.dto.UserSettingsDto;
import com.trustmarket.handshake.user.mapper.UserMapper;
import com.trustmarket.handshake.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(UserMapper.toResponse(userService.getById(userId)));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(Authentication auth, @Valid @RequestBody UpdateUserRequest req) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(UserMapper.toResponse(
                userService.updateBasics(userId, req.getFirstName(), req.getLastName(), req.getPhone(), req.getProfilePictureUrl())
        ));
    }

    // public profile (for marketplace)
    @GetMapping("/{id}/profile")
    public ApiResponse<UserResponse> publicProfile(@PathVariable UUID id) {
        return ApiResponse.ok(UserMapper.toResponse(userService.getById(id)));
    }

    @GetMapping("/me/notification-settings")
    public ApiResponse<UserSettingsDto> getNotificationSettings(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userService.getNotificationSettings(userId));
    }

    @PutMapping("/me/notification-settings")
    public ApiResponse<UserSettingsDto> updateNotificationSettings(Authentication auth, @RequestBody UserSettingsDto dto) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userService.updateNotificationSettings(userId, dto));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        userService.deleteAccount(userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me/export")
    public ApiResponse<Object> exportMe(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userService.exportAccount(userId));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> uploadAvatar(Authentication auth, @RequestParam("file") MultipartFile file) {
        UUID userId = AuthUtils.userId(auth);
        String url = userService.uploadAvatar(userId, file);
        return ApiResponse.ok(url);
    }
}
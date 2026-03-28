package com.trustmarket.handshake.auth.controller;

import com.trustmarket.handshake.auth.dto.*;
import com.trustmarket.handshake.auth.service.AuthService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.JwtService;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(
                req.getEmail(),
                req.getPassword(),
                req.getPasswordConfirm(),
                req.getFirstName(),
                req.getLastName(),
                req.getPhone(),
                req.getCity(),
                req.getProvince()
        );

        String access = authService.newAccessToken(user);
        String refresh = authService.issueAndStoreRefreshToken(user);

        return ApiResponse.ok(buildAuthResponse(user, access, refresh));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = authService.authenticate(req.getEmail(), req.getPassword());

        String access = authService.newAccessToken(user);
        String refresh = authService.issueAndStoreRefreshToken(user);

        return ApiResponse.ok(buildAuthResponse(user, access, refresh));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String oldRefresh = req.getRefreshToken();

        // Rotate & persist
        String newRefresh = authService.rotateRefreshToken(oldRefresh);

        // New access token from new refresh claims (or fetch user; we’ll derive from token)
        Claims claims = jwtService.parse(newRefresh);
        String email = claims.getSubject();
        String role = (String) claims.get("role");
        String userId = (String) claims.get("userId");

        User user = userService.getById(UUID.fromString(userId));

        String newAccess = jwtService.generateAccessToken(UUID.fromString(userId), email, role);

        return ApiResponse.ok(buildAuthResponse(user, newAccess, newRefresh));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return ApiResponse.ok(null);
    }


    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getToken(), req.getNewPassword(), req.getConfirmPassword());
        return ApiResponse.ok(null);
    }


    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.changePassword(userId, req.getOldPassword(), req.getNewPassword(), req.getConfirmPassword());
        return ApiResponse.ok(null);
    }


    @PostMapping("/2fa-enable")
    public ApiResponse<Void> enable2fa(@Valid @RequestBody TwoFaEnableRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.enable2fa(userId, req.getMethod());
        return ApiResponse.ok(null);
    }


    @PostMapping("/2fa-disable")
    public ApiResponse<Void> disable2fa(@RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.disable2fa(userId);
        return ApiResponse.ok(null);
    }


    @PostMapping("/2fa-verify")
    public ApiResponse<Void> verify2fa(@Valid @RequestBody TwoFaVerifyRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.verify2fa(userId, req.getCode());
        return ApiResponse.ok(null);
    }


    @GetMapping("/2fa-status")
    public ApiResponse<Boolean> get2faStatus(@RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        boolean enabled = authService.get2faStatus(userId);
        return ApiResponse.ok(enabled);
    }

    private UUID extractUserIdFromJwt(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtService.parse(token);
        String userId = (String) claims.get("userId");
        return UUID.fromString(userId);
    }
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresInSeconds(authService.accessExpiresInSeconds())
                .refreshExpiresInSeconds(authService.refreshExpiresInSeconds())
                .build();
    }
}
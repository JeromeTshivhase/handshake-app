package com.trustmarket.handshake.auth.controller;

import com.trustmarket.handshake.auth.dto.*;
import com.trustmarket.handshake.auth.service.AuthService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.rate.RateLimiterService;
import com.trustmarket.handshake.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req, @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor, @RequestHeader(value = "Remote_Addr", required = false) String remoteAddr) {
        String ip = xForwardedFor != null ? xForwardedFor : remoteAddr;
        Bucket bucket = rateLimiterService.resolveBucket("register:" + (ip != null ? ip : "unknown"));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many registration attempts. Please try again later.");
        }
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req, @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor, @RequestHeader(value = "Remote_Addr", required = false) String remoteAddr) {
        String ip = xForwardedFor != null ? xForwardedFor : remoteAddr;
        Bucket bucket = rateLimiterService.resolveBucket("login:" + (ip != null ? ip : "unknown"));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.");
        }
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(authService.refresh(req));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req, @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor, @RequestHeader(value = "Remote_Addr", required = false) String remoteAddr) {
        String ip = xForwardedFor != null ? xForwardedFor : remoteAddr;
        Bucket bucket = rateLimiterService.resolveBucket("forgot-password:" + (ip != null ? ip : "unknown"));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts. Please try again later.");
        }
        authService.forgotPassword(req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req, @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor, @RequestHeader(value = "Remote_Addr", required = false) String remoteAddr) {
        String ip = xForwardedFor != null ? xForwardedFor : remoteAddr;
        Bucket bucket = rateLimiterService.resolveBucket("reset-password:" + (ip != null ? ip : "unknown"));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts. Please try again later.");
        }
        authService.resetPassword(req);
        return ApiResponse.ok(null);
    }


    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.changePassword(userId, req);
        return ApiResponse.ok(null);
    }


    @PostMapping("/2fa/setup")
    public ApiResponse<TwoFaSetupResponse> setup2fa(@Valid @RequestBody TwoFaSetupRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        return ApiResponse.ok(authService.setup2fa(userId, req));
    }

    @PostMapping("/2fa/enable")
    public ApiResponse<java.util.List<String>> enable2fa(@Valid @RequestBody TwoFaEnableRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        return ApiResponse.ok(authService.enable2fa(userId, req));
    }

    @PostMapping("/2fa/disable")
    public ApiResponse<Void> disable2fa(@Valid @RequestBody PasswordRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.disable2fa(userId, req.getPassword());
        return ApiResponse.ok(null);
    }


    @PostMapping("/2fa/verify")
    public ApiResponse<Void> verify2fa(@Valid @RequestBody TwoFaVerifyRequest req, @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        authService.verify2fa(userId, req);
        return ApiResponse.ok(null);
    }


    @GetMapping("/2fa/status")
    public ApiResponse<TwoFaStatusResponse> get2faStatus(@RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        return ApiResponse.ok(authService.get2faStatus(userId));
    }

    @PostMapping("/2fa/recovery/regenerate")
    public ApiResponse<java.util.List<String>> regenerateRecoveryCodes(@Valid @RequestBody VerificationCodeRequest req,
                                                                       @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserIdFromJwt(authHeader);
        return ApiResponse.ok(authService.regenerateRecoveryCodes(userId, req.getCode()));
    }

    private UUID extractUserIdFromJwt(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtService.parse(token);
        String userId = (String) claims.get("userId");
        return UUID.fromString(userId);
    }
    // ...existing code...
}


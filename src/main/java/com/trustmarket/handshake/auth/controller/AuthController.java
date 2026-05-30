package com.trustmarket.handshake.auth.controller;

import com.trustmarket.handshake.auth.dto.*;
import com.trustmarket.handshake.auth.service.AuthService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.rate.RateLimiterService;
import com.trustmarket.handshake.shared.security.JwtService;
import com.trustmarket.handshake.shared.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                              HttpServletRequest request) {
        Bucket bucket = rateLimiterService.resolveBucket("register:" + resolveClientIp(request));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many registration attempts. Please try again later.");
        }
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                           HttpServletRequest request) {
        Bucket bucket = rateLimiterService.resolveBucket("login:" + resolveClientIp(request));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.");
        }
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req,
                                             HttpServletRequest request) {
        Bucket bucket = rateLimiterService.resolveBucket("refresh:" + resolveClientIp(request));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many token refresh attempts. Please try again later.");
        }
        return ApiResponse.ok(authService.refresh(req));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                            HttpServletRequest request) {
        Bucket bucket = rateLimiterService.resolveBucket("forgot-password:" + resolveClientIp(request));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts. Please try again later.");
        }
        authService.forgotPassword(req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req,
                                           HttpServletRequest request) {
        Bucket bucket = rateLimiterService.resolveBucket("reset-password:" + resolveClientIp(request));
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset attempts. Please try again later.");
        }
        authService.resetPassword(req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        authService.changePassword(userId, req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/2fa/setup")
    public ApiResponse<TwoFaSetupResponse> setup2fa(@Valid @RequestBody TwoFaSetupRequest req, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ApiResponse.ok(authService.setup2fa(userId, req));
    }

    @PostMapping("/2fa/enable")
    public ApiResponse<java.util.List<String>> enable2fa(@Valid @RequestBody TwoFaEnableRequest req, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ApiResponse.ok(authService.enable2fa(userId, req));
    }

    @PostMapping("/2fa/verify")
    public ApiResponse<Void> verify2fa(@Valid @RequestBody TwoFaVerifyRequest req, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        authService.verify2fa(userId, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/2fa/status")
    public ApiResponse<TwoFaStatusResponse> get2faStatus(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ApiResponse.ok(authService.get2faStatus(userId));
    }

    @PostMapping("/2fa/recovery/regenerate")
    public ApiResponse<java.util.List<String>> regenerateRecoveryCodes(@Valid @RequestBody VerificationCodeRequest req, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ApiResponse.ok(authService.regenerateRecoveryCodes(userId, req.getCode()));
    }

    @PostMapping("/2fa/disable")
    public ApiResponse<Void> disable2fa(@Valid @RequestBody PasswordRequest req, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        authService.disable2fa(userId, req.getPassword());
        return ApiResponse.ok(null);
    }

    // Helpers
    private UUID extractUserIdFromJwt(String authHeader) {
        if (authHeader == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtService.parse(token);
        String userId = (String) claims.get("userId");
        return UUID.fromString(userId);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication");
        }
        Object p = authentication.getPrincipal();
        if (p instanceof UUID) {
            return (UUID) p;
        }
        if (p instanceof UserPrincipal) {
            return ((UserPrincipal) p).getId();
        }
        if (p instanceof String) {
            try {
                return UUID.fromString((String) p);
            } catch (IllegalArgumentException ignored) {
            }
            // try parsing as JWT
            try {
                String token = ((String) p).replace("Bearer ", "");
                Claims claims = jwtService.parse(token);
                String userId = (String) claims.get("userId");
                return UUID.fromString(userId);
            } catch (Exception ignored) {
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to resolve user id from authentication");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
    }
}

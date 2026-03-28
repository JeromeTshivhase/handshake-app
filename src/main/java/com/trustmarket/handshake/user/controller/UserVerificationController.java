package com.trustmarket.handshake.user.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.user.dto.UserVerificationStatusResponse;
import com.trustmarket.handshake.user.dto.VerificationDispatchResponse;
import com.trustmarket.handshake.user.dto.VerifyOtpRequest;
import com.trustmarket.handshake.user.service.UserVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/verifications")
@RequiredArgsConstructor
public class UserVerificationController {

    private final UserVerificationService userVerificationService;

    @GetMapping
    public ApiResponse<UserVerificationStatusResponse> status(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userVerificationService.getStatus(userId));
    }

    @PostMapping("/email/request")
    public ApiResponse<VerificationDispatchResponse> requestEmailOtp(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userVerificationService.requestEmailOtp(userId));
    }

    @PostMapping("/email/verify")
    public ApiResponse<UserVerificationStatusResponse> verifyEmailOtp(Authentication auth,
                                                                      @Valid @RequestBody VerifyOtpRequest req) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userVerificationService.verifyEmailOtp(userId, req.getOtp()));
    }

    @PostMapping("/phone/request")
    public ApiResponse<VerificationDispatchResponse> requestPhoneOtp(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userVerificationService.requestPhoneOtp(userId));
    }

    @PostMapping("/phone/verify")
    public ApiResponse<UserVerificationStatusResponse> verifyPhoneOtp(Authentication auth,
                                                                      @Valid @RequestBody VerifyOtpRequest req) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(userVerificationService.verifyPhoneOtp(userId, req.getOtp()));
    }
}


package com.trustmarket.handshake.safe_meetup.controller;

import com.trustmarket.handshake.safe_meetup.dto.*;
import com.trustmarket.handshake.safe_meetup.service.MeetupService;
import com.trustmarket.handshake.safe_meetup.service.OtpRedisStore;
import com.trustmarket.handshake.safe_meetup.service.SafeMeetupOtpService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.transaction.entity.Handshake;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/handshakes/{handshakeId}")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupService meetupService;
    private final SafeMeetupOtpService safeMeetupOtpService;
    private final OtpRedisStore otpRedisStore;

    @PostMapping("/meetup")
    public ApiResponse<MeetupResponse> createOrUpdateMeetup(
            @PathVariable UUID handshakeId,
            Authentication auth,
            @Valid @RequestBody CreateMeetupRequest req
    ) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(MeetupResponse.from(meetupService.createOrUpdate(handshakeId, userId, req)));
    }

    @GetMapping("/meetup")
    public ApiResponse<MeetupResponse> getMeetup(@PathVariable UUID handshakeId, Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(MeetupResponse.from(meetupService.get(handshakeId, userId)));
    }

    @PostMapping("/otp/enable")
    public ApiResponse<OtpEnableResponse> enableOtp(@PathVariable UUID handshakeId, Authentication auth, @Valid @RequestBody GeoPointRequest req) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(safeMeetupOtpService.enableIfEligible(handshakeId, userId, req.getLat(), req.getLng()));
    }

    /**
     * Buyer reveals OTP only when enabled.
     * OTP is fetched from Redis (plaintext) to allow reveal.
     */
    @GetMapping("/otp")
    public ApiResponse<OtpRevealResponse> revealOtp(@PathVariable UUID handshakeId, Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        // Only allow the buyer to see the OTP
        Handshake handshake = meetupService.getHandshake(handshakeId);
        if (!handshake.getBuyer().getId().equals(userId)) {
            throw new ForbiddenException("Only the buyer can view the OTP");
        }
        String otp = otpRedisStore.get(handshakeId)
                .orElseThrow(() -> new BadRequestException("OTP not available (may have expired). Ask to re-accept handshake or contact support."));
        return ApiResponse.ok(OtpRevealResponse.builder()
                .enabled(true)
                .otp(otp)
                .expiresAt(handshake.getOtpExpiresAt())
                .build());
    }

    @PostMapping("/otp/verify")
    public ApiResponse<Void> verifyOtp(@PathVariable UUID handshakeId, Authentication auth, @Valid @RequestBody OtpVerifyRequest req) {
        UUID sellerId = AuthUtils.userId(auth);
        safeMeetupOtpService.verifyOtpBySeller(handshakeId, sellerId, req.getOtp(), req.getLat(), req.getLng());
        return ApiResponse.ok(null);
    }
}
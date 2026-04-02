package com.trustmarket.handshake.safe_meetup.controller;

import com.trustmarket.handshake.safe_meetup.dto.CheckinRequest;
import com.trustmarket.handshake.safe_meetup.dto.CreateMeetupRequest;
import com.trustmarket.handshake.safe_meetup.dto.GeoPointRequest;
import com.trustmarket.handshake.safe_meetup.dto.MeetupResponse;
import com.trustmarket.handshake.safe_meetup.dto.OtpEnableResponse;
import com.trustmarket.handshake.safe_meetup.dto.OtpRevealResponse;
import com.trustmarket.handshake.safe_meetup.dto.OtpVerifyRequest;
import com.trustmarket.handshake.safe_meetup.service.MeetupService;
import com.trustmarket.handshake.safe_meetup.service.SafeMeetupOtpService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/handshakes/{handshakeId}")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupService meetupService;
    private final SafeMeetupOtpService safeMeetupOtpService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/meetup")
    public ApiResponse<MeetupResponse> createOrUpdateMeetup(
            @PathVariable UUID handshakeId,
            Authentication auth,
            @Valid @RequestBody CreateMeetupRequest req
    ) {
        UUID userId = AuthUtils.userId(auth);
        var meetup = meetupService.createOrUpdate(handshakeId, userId, req);
        var txnId = transactionRepository.findByHandshake_Id(handshakeId).map(t -> t.getId()).orElse(null);
        return ApiResponse.ok(MeetupResponse.from(meetup, txnId));
    }

    @GetMapping("/meetup")
    public ApiResponse<MeetupResponse> getMeetup(@PathVariable UUID handshakeId, Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        var meetup = meetupService.get(handshakeId, userId);
        var txnId = transactionRepository.findByHandshake_Id(handshakeId).map(t -> t.getId()).orElse(null);
        return ApiResponse.ok(MeetupResponse.from(meetup, txnId));
    }

    @PutMapping("/meetup")
    public ApiResponse<MeetupResponse> updateMeetup(
            @PathVariable UUID handshakeId,
            Authentication auth,
            @Valid @RequestBody CreateMeetupRequest req
    ) {
        UUID userId = AuthUtils.userId(auth);
        var meetup = meetupService.createOrUpdate(handshakeId, userId, req);
        var txnId = transactionRepository.findByHandshake_Id(handshakeId).map(t -> t.getId()).orElse(null);
        return ApiResponse.ok(MeetupResponse.from(meetup, txnId));
    }

    @DeleteMapping("/meetup")
    public ApiResponse<Void> cancelMeetup(@PathVariable UUID handshakeId, Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        meetupService.cancelMeetup(handshakeId, userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/otp/enable")
    public ApiResponse<OtpEnableResponse> enableOtp(
            @PathVariable UUID handshakeId,
            Authentication auth,
            @Valid @RequestBody GeoPointRequest req
    ) {
        UUID userId = AuthUtils.userId(auth);
        return ApiResponse.ok(safeMeetupOtpService.enableIfEligible(handshakeId, userId, req.getLat(), req.getLng()));
    }

    @GetMapping("/otp")
    public ApiResponse<OtpRevealResponse> revealOtp(@PathVariable UUID handshakeId, Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        String otp = safeMeetupOtpService.revealOtpToBuyer(handshakeId, userId, null);
        var handshake = meetupService.getHandshake(handshakeId);
        return ApiResponse.ok(OtpRevealResponse.builder()
                .enabled(true)
                .otp(otp)
                .expiresAt(handshake.getOtpExpiresAt())
                .warning("Show this code to the seller to complete the transaction")
                .build());
    }

    @PostMapping("/otp/verify")
    public ApiResponse<Void> verifyOtp(
            @PathVariable UUID handshakeId,
            Authentication auth,
            @Valid @RequestBody OtpVerifyRequest req
    ) {
        UUID sellerId = AuthUtils.userId(auth);
        safeMeetupOtpService.verifyOtpBySeller(handshakeId, sellerId, req.getOtp(), req.getLat(), req.getLng());
        return ApiResponse.ok(null);
    }

    @PostMapping("/checkin")
    public ApiResponse<Void> checkin(
            @PathVariable UUID handshakeId,
            Authentication auth,
            @Valid @RequestBody CheckinRequest req
    ) {
        UUID userId = AuthUtils.userId(auth);
        safeMeetupOtpService.checkin(handshakeId, userId, req);
        return ApiResponse.ok(null);
    }
}
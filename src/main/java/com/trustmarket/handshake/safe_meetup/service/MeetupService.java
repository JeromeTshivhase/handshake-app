package com.trustmarket.handshake.safe_meetup.service;

import com.trustmarket.handshake.safe_meetup.dto.CreateMeetupRequest;
import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
import com.trustmarket.handshake.safe_meetup.repository.MeetupRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetupService {

    private final MeetupRepository meetupRepository;
    private final HandshakeRepository handshakeRepository;

    @Transactional
    public Meetup createOrUpdate(UUID handshakeId, UUID userId, CreateMeetupRequest req) {
        Handshake h = getHandshake(handshakeId);
        validateMeetupAccess(h, userId);
        validateHandshakeStateForMeetup(h);

        if (!isValidLocation(req.getLatitude(), req.getLongitude())) {
            throw new BadRequestException("Invalid location coordinates");
        }

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseGet(() -> Meetup.builder().handshake(h).build());

        if (meetup.getStatus() == MeetupStatus.COMPLETED || meetup.getStatus() == MeetupStatus.CANCELLED) {
            throw new BadRequestException("Meetup can no longer be modified in state " + meetup.getStatus());
        }

        meetup.setLatitude(req.getLatitude());
        meetup.setLongitude(req.getLongitude());
        meetup.setAddressLabel(req.getAddressLabel());
        meetup.setScheduledTime(req.getScheduledTime());
        meetup.setStatus(MeetupStatus.SCHEDULED);

        // Any location/time update invalidates active OTP material.
        meetup.setOtpCodeHash(null);
        meetup.setOtpEnabledAt(null);
        meetup.setOtpExpiresAt(null);
        meetup.setOtpFailedAttempts(0);
        meetup.setOtpLockedUntil(null);

        return meetupRepository.save(meetup);
    }

    @Transactional(readOnly = true)
    public Meetup get(UUID handshakeId, UUID userId) {
        Handshake h = getHandshake(handshakeId);
        validateMeetupAccess(h, userId);

        return meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new NotFoundException("Meetup not scheduled for handshake: " + handshakeId));
    }

    public Handshake getHandshake(UUID handshakeId) {
        return handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));
    }

    @Transactional
    public void cancelMeetup(UUID handshakeId, UUID userId) {
        Handshake h = getHandshake(handshakeId);
        validateMeetupAccess(h, userId);

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new NotFoundException("Meetup not scheduled for handshake: " + handshakeId));

        if (meetup.getStatus() == MeetupStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed meetup");
        }
        if (meetup.getStatus() == MeetupStatus.EXPIRED) {
            throw new BadRequestException("Meetup already expired");
        }

        meetup.setStatus(MeetupStatus.CANCELLED);
        meetup.setOtpCodeHash(null);
        meetup.setOtpEnabledAt(null);
        meetup.setOtpExpiresAt(null);
        meetupRepository.save(meetup);
    }

    private void validateMeetupAccess(Handshake handshake, UUID userId) {
        boolean isBuyer = handshake.getBuyer().getId().equals(userId);
        boolean isSeller = handshake.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("Not allowed");
        }
    }

    private void validateHandshakeStateForMeetup(Handshake handshake) {
        if (handshake.getStatus() != HandshakeStatus.ESCROW_HELD) {
            throw new BadRequestException("Meetup can only be scheduled after handshake is ESCROW_HELD");
        }
    }

    private boolean isValidLocation(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}
package com.trustmarket.handshake.safe_meetup.service;

import com.trustmarket.handshake.safe_meetup.dto.CreateMeetupRequest;
import com.trustmarket.handshake.safe_meetup.entity.Meetup;
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
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        // Only buyer/seller can set meetup
        boolean allowed = h.getBuyer().getId().equals(userId) || h.getSeller().getId().equals(userId);
        if (!allowed) throw new ForbiddenException("Not allowed");

        if (h.getStatus() != HandshakeStatus.ESCROW_HELD) {
            throw new BadRequestException("Meetup can only be scheduled after handshake ACCEPTED");
        }

        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseGet(() -> Meetup.builder().handshake(h).build());

        meetup.setLatitude(req.getLatitude());
        meetup.setLongitude(req.getLongitude());
        meetup.setAddressLabel(req.getAddressLabel());
        meetup.setScheduledTime(req.getScheduledTime());

        return meetupRepository.save(meetup);
    }

    @Transactional(readOnly = true)
    public Meetup get(UUID handshakeId, UUID userId) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));

        boolean allowed = h.getBuyer().getId().equals(userId) || h.getSeller().getId().equals(userId);
        if (!allowed) throw new ForbiddenException("Not allowed");

        return meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new NotFoundException("Meetup not scheduled for handshake: " + handshakeId));
    }

    public Handshake getHandshake(UUID handshakeId) {
        return handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));
    }

    @Transactional
    public void cancelMeetup(UUID handshakeId, UUID userId) {
        Handshake h = handshakeRepository.findById(handshakeId)
                .orElseThrow(() -> new NotFoundException("Handshake not found: " + handshakeId));
        boolean allowed = h.getBuyer().getId().equals(userId) || h.getSeller().getId().equals(userId);
        if (!allowed) throw new ForbiddenException("Not allowed");
        Meetup meetup = meetupRepository.findByHandshake_Id(handshakeId)
                .orElseThrow(() -> new NotFoundException("Meetup not scheduled for handshake: " + handshakeId));
        meetup.setStatus(com.trustmarket.handshake.safe_meetup.entity.MeetupStatus.CANCELLED);
        meetupRepository.save(meetup);
    }
}
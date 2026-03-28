package com.trustmarket.handshake.safe_meetup.repository;

import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetupRepository extends JpaRepository<Meetup, UUID> {
    Optional<Meetup> findByHandshake_Id(UUID handshakeId);
}
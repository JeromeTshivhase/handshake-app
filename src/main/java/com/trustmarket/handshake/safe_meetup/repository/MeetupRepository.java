package com.trustmarket.handshake.safe_meetup.repository;

import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MeetupRepository extends JpaRepository<Meetup, UUID> {
    Optional<Meetup> findByHandshake_Id(UUID handshakeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Meetup m WHERE m.handshake.id = :handshakeId")
    Optional<Meetup> findByHandshakeIdForUpdate(@Param("handshakeId") UUID handshakeId);

    @Modifying
    @Query("""
        UPDATE Meetup m
        SET m.status = :newStatus
        WHERE m.status IN ('SCHEDULED', 'READY')
          AND m.scheduledTime < :cutoff
        """)
    int expireMeetupsByTime(@Param("cutoff") LocalDateTime cutoff, @Param("newStatus") MeetupStatus newStatus);

    @Modifying
    @Query("""
        UPDATE Meetup m
        SET m.status = :newStatus
        WHERE m.status = 'ACTIVE'
          AND m.otpExpiresAt IS NOT NULL
          AND m.otpExpiresAt < :cutoff
        """)
    int expireMeetupsByOtpExpiry(@Param("cutoff") LocalDateTime cutoff, @Param("newStatus") MeetupStatus newStatus);
}
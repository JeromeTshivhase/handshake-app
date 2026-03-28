package com.trustmarket.handshake.user.repository;

import com.trustmarket.handshake.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    long deleteByExpiresAtBefore(LocalDateTime now);
}
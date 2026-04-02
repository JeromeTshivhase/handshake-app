package com.trustmarket.handshake.messaging.repository;

import com.trustmarket.handshake.messaging.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);

    long deleteByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);
}


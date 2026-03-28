package com.trustmarket.handshake.user.repository;

import com.trustmarket.handshake.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
package com.trustmarket.handshake.user.repository;

import com.trustmarket.handshake.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {
}
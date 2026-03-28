package com.trustmarket.handshake.user.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsDto {
    private UUID userId;
    private boolean notificationsEnabled;
    private boolean emailNotifications;
    private boolean darkMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



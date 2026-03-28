
package com.trustmarket.handshake.user.dto;

import lombok.Data;

@Data
public class UpdateSettingsRequest {
    private Boolean notificationsEnabled;
    private Boolean emailNotifications;
    private Boolean darkMode;
}


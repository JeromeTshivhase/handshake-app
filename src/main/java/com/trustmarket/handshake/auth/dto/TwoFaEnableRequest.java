package com.trustmarket.handshake.auth.dto;

import lombok.Data;

@Data
public class TwoFaEnableRequest {
    private String method; // e.g., "app", "sms", "email"
}


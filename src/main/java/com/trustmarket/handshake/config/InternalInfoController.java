package com.trustmarket.handshake.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/internal")
public class InternalInfoController {

    @Value("${otp.expires-minutes:10}")
    private long otpExpiresMinutes;

    @Value("${handshake.pending-expiry-hours:48}")
    private int handshakePendingExpiryHours;

    @Value("${meetup.cleanup.cron:0 * * * * *}")
    private String meetupCleanupCron;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> m = new HashMap<>();
        m.put("jvmTimezone", TimeZone.getDefault().getID());
        m.put("APP_TIMEZONE_env", System.getenv("APP_TIMEZONE"));
        m.put("jackson_time_zone_property", System.getProperty("APP_TIMEZONE"));
        m.put("otpExpiresMinutes", otpExpiresMinutes);
        m.put("handshakePendingExpiryHours", handshakePendingExpiryHours);
        m.put("meetupCleanupCron", meetupCleanupCron);
        return ResponseEntity.ok(m);
    }
}


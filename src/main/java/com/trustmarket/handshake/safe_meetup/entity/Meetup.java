package com.trustmarket.handshake.safe_meetup.entity;

import com.trustmarket.handshake.transaction.entity.Handshake;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meetup", indexes = {
        @Index(name = "idx_meetup_handshake", columnList = "handshake_id"),
        @Index(name = "idx_meetup_status_time", columnList = "status,scheduled_time"),
        @Index(name = "idx_meetup_otp_expiry", columnList = "otp_expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meetup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handshake_id", nullable = false, unique = true)
    private Handshake handshake;

    @Column(name = "safe_location_id")
    private UUID safeLocationId; // reserved for future safe-location catalog

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "address_label", length = 255)
    private String addressLabel;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "time_window_minutes", nullable = false)
    @Builder.Default
    private Integer timeWindowMinutes = 30;

    @Column(name = "radius_meters", nullable = false)
    @Builder.Default
    private Integer radiusMeters = 200;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MeetupStatus status = MeetupStatus.SCHEDULED;

    @Column(name = "otp_code_hash", length = 255)
    private String otpCodeHash;

    @Column(name = "otp_enabled_at")
    private LocalDateTime otpEnabledAt;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_failed_attempts", nullable = false)
    @Builder.Default
    private Integer otpFailedAttempts = 0;

    @Column(name = "otp_locked_until")
    private LocalDateTime otpLockedUntil;

    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    @Column(name = "buyer_last_latitude")
    private Double buyerLastLatitude;

    @Column(name = "buyer_last_longitude")
    private Double buyerLastLongitude;

    @Column(name = "buyer_last_checkin")
    private LocalDateTime buyerLastCheckin;

    @Column(name = "seller_last_latitude")
    private Double sellerLastLatitude;

    @Column(name = "seller_last_longitude")
    private Double sellerLastLongitude;

    @Column(name = "seller_last_checkin")
    private LocalDateTime sellerLastCheckin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isOtpLocked() {
        return otpLockedUntil != null && otpLockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isOtpExpired() {
        return otpExpiresAt != null && otpExpiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isOtpEnabled() {
        return otpCodeHash != null && otpEnabledAt != null && !isOtpExpired();
    }

    public boolean isWithinTimeWindow(LocalDateTime now) {
        LocalDateTime start = scheduledTime.minusMinutes(timeWindowMinutes);
        LocalDateTime end = scheduledTime.plusMinutes(timeWindowMinutes);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public boolean isWithinRadius(double lat, double lng) {
        return calculateDistanceMeters(lat, lng, latitude, longitude) <= radiusMeters;
    }

    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
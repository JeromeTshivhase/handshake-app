package com.trustmarket.handshake.transaction.entity;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "handshake", indexes = {
        @Index(name = "idx_handshake_listing", columnList = "listing_id"),
        @Index(name = "idx_handshake_buyer", columnList = "buyer_id"),
        @Index(name = "idx_handshake_seller", columnList = "seller_id"),
        @Index(name = "idx_handshake_status", columnList = "status"),
        @Index(name = "idx_handshake_otp_enabled", columnList = "otp_enabled"),
        @Index(name = "idx_handshake_otp_verified", columnList = "otp_verified")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Handshake {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Optimistic lock — prevents lost updates under concurrent modification. */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "agreed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private HandshakeStatus status = HandshakeStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "otp_code_hash", length = 255)
    private String otpCodeHash;

    @Column(name = "otp_reveal_ciphertext", length = 512)
    private String otpRevealCiphertext;

    @Column(name = "otp_enabled", nullable = false)
    @Builder.Default
    private boolean otpEnabled = false;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_verified", nullable = false)
    @Builder.Default
    private boolean otpVerified = false;

    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    @Column(name = "otp_failed_attempts", nullable = false)
    @Builder.Default
    private int otpFailedAttempts = 0;

    @Column(name = "otp_locked_until")
    private LocalDateTime otpLockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isActiveLike() {
        return status == HandshakeStatus.PENDING || status == HandshakeStatus.ACCEPTED;
    }

    public boolean isOtpLockedNow() {
        return otpLockedUntil != null && otpLockedUntil.isAfter(LocalDateTime.now());
    }
}
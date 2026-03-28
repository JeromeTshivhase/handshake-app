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
        @Index(name = "idx_meetup_scheduled_time", columnList = "scheduled_time"),
        @Index(name = "idx_meetup_status", columnList = "status")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MeetupStatus status = MeetupStatus.SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
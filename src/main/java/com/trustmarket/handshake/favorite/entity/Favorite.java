package com.trustmarket.handshake.favorite.entity;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_user_listing",
                columnNames = {"user_id", "listing_id"}
        ),
        indexes = {
                @Index(name = "idx_fav_user", columnList = "user_id"),
                @Index(name = "idx_fav_listing", columnList = "listing_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
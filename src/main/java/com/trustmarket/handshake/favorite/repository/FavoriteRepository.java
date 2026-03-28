package com.trustmarket.handshake.favorite.repository;

import com.trustmarket.handshake.favorite.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    Optional<Favorite> findByUser_IdAndListing_Id(UUID userId, UUID listingId);
    Page<Favorite> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
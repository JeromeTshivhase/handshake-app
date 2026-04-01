package com.trustmarket.handshake.listing.repository;

import com.trustmarket.handshake.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingImageRepository extends JpaRepository<ListingImage, UUID> {
    List<ListingImage> findByListing_IdOrderByPositionAsc(UUID listingId);
    Optional<ListingImage> findByIdAndListing_Id(UUID id, UUID listingId);
    void deleteByListing_Id(UUID listingId);
}
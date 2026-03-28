package com.trustmarket.handshake.listing.repository;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    Page<Listing> findBySeller_Id(UUID sellerId, Pageable pageable);

    // FIXED VERSION
    @Query("""
        SELECT l FROM Listing l
        LEFT JOIN l.category c
        WHERE l.status = 'ACTIVE'
          AND (:keyword IS NULL OR 
               LOWER(l.title) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')) OR 
               LOWER(l.description) LIKE LOWER(CONCAT('%', COALESCE(:keyword, ''), '%')))
          AND (:categoryId IS NULL OR l.category.id = :categoryId)
          AND (:categoryName IS NULL OR 
               LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', COALESCE(:categoryName, ''), '%')))
          AND (:minPrice IS NULL OR l.price >= :minPrice)
          AND (:maxPrice IS NULL OR l.price <= :maxPrice)
          AND (:location IS NULL OR 
               LOWER(COALESCE(l.location, '')) LIKE LOWER(CONCAT('%', COALESCE(:location, ''), '%')))
        """)
    Page<Listing> searchActive(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("categoryName") String categoryName,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("location") String location,
            Pageable pageable
    );
}
package com.trustmarket.handshake.transaction.repository;

import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HandshakeRepository extends JpaRepository<Handshake, UUID> {

    @Query("""
        SELECT h FROM Handshake h
        WHERE (h.buyer.id = :userId OR h.seller.id = :userId)
        AND (:status IS NULL OR h.status = :status)
        ORDER BY h.createdAt DESC
        """)
    org.springframework.data.domain.Page<Handshake> findByUserIdAndStatus(
        @Param("userId") UUID userId,
        @Param("status") HandshakeStatus status,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        SELECT h FROM Handshake h
        WHERE h.listing.id = :listingId
          AND h.status IN ('PENDING', 'ACCEPTED')
        """)
    Optional<Handshake> findActiveByListingId(UUID listingId);

    long countByListing_IdAndStatus(UUID listingId, HandshakeStatus status);

    @Query("""
        SELECT h FROM Handshake h
        WHERE h.listing.id = :listingId
          AND h.buyer.id = :buyerId
          AND h.seller.id = :sellerId
        ORDER BY h.createdAt DESC
        """)
    Optional<Handshake> findLatestByListingAndBuyerAndSeller(@Param("listingId") UUID listingId,
                                                             @Param("buyerId") UUID buyerId,
                                                             @Param("sellerId") UUID sellerId);

    @Query("""
        SELECT h FROM Handshake h
        WHERE h.seller.id = :sellerId
        AND (:status IS NULL OR h.status = :status)
        ORDER BY h.createdAt DESC
        """)
    org.springframework.data.domain.Page<Handshake> findBySellerIdAndStatus(
        @Param("sellerId") UUID sellerId,
        @Param("status") HandshakeStatus status,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        SELECT h FROM Handshake h
        WHERE h.buyer.id = :buyerId
        AND (:status IS NULL OR h.status = :status)
        ORDER BY h.createdAt DESC
        """)
    org.springframework.data.domain.Page<Handshake> findByBuyerIdAndStatus(
        @Param("buyerId") UUID buyerId,
        @Param("status") HandshakeStatus status,
        org.springframework.data.domain.Pageable pageable
    );

    List<Handshake> findAllByStatusAndCreatedAtBefore(HandshakeStatus status, LocalDateTime before);
    List<Handshake> findAllByStatusAndUpdatedAtBefore(HandshakeStatus status, LocalDateTime before);
    List<Handshake> findAllByStatusAndCreatedAtBeforeAndUpdatedAtBefore(HandshakeStatus status, LocalDateTime createdAt, LocalDateTime updatedAt);

    List<Handshake> findByListing_IdAndBuyer_IdAndStatusIn(UUID listingId, UUID buyerId, List<HandshakeStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Handshake h WHERE h.id = :id")
    Optional<Handshake> findByIdForOtpVerification(@Param("id") UUID id);
}

package com.trustmarket.handshake.messaging.repository;

import com.trustmarket.handshake.messaging.entity.Conversation;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @Query("""
        SELECT c FROM Conversation c
        JOIN FETCH c.listing
        JOIN FETCH c.buyer
        JOIN FETCH c.seller
        WHERE c.id = :id
    """)
    Optional<Conversation> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.listing.id = :listingId
          AND c.buyer.id = :buyerId
          AND c.seller.id = :sellerId
        """)
    Optional<Conversation> findUnique(@Param("listingId") UUID listingId,
                                      @Param("buyerId") UUID buyerId,
                                      @Param("sellerId") UUID sellerId);

    @Query("""
        SELECT c FROM Conversation c
        WHERE (c.buyer.id = :userId OR c.seller.id = :userId)
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
        """)
    Page<Conversation> findForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.listing.id = :listingId
          AND (c.buyer.id = :userId OR c.seller.id = :userId)
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
        """)
    Page<Conversation> findForUserAndListing(@Param("userId") UUID userId,
                                             @Param("listingId") UUID listingId,
                                             Pageable pageable);
}

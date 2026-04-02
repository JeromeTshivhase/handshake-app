package com.trustmarket.handshake.messaging.repository;

import com.trustmarket.handshake.messaging.entity.Conversation;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
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

    /**
     * Fetch conversations for a user.
     * Uses @EntityGraph to avoid N+1 on listing/buyer/seller — Spring Data executes
     * a separate count query so the join-fetch doesn't conflict with pagination.
     */
    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    @Query("""
        SELECT c FROM Conversation c
        WHERE (c.buyer.id = :userId OR c.seller.id = :userId)
          AND c.status <> 'BLOCKED'
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
        """)
    Page<Conversation> findForUser(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    @Query("""
        SELECT c FROM Conversation c
        WHERE c.listing.id = :listingId
          AND (c.buyer.id = :userId OR c.seller.id = :userId)
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
        """)
    Page<Conversation> findForUserAndListing(@Param("userId") UUID userId,
                                             @Param("listingId") UUID listingId,
                                             Pageable pageable);

    /**
     * Atomic unread-count increment — avoids the read-modify-write race condition.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Conversation c SET c.unreadCount = c.unreadCount + 1 WHERE c.id = :id")
    int incrementUnreadCount(@Param("id") UUID id);

    @Query("""
        SELECT c FROM Conversation c
        WHERE (c.buyer.id = :userA AND c.seller.id = :userB)
           OR (c.buyer.id = :userB AND c.seller.id = :userA)
        """)
    List<Conversation> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}


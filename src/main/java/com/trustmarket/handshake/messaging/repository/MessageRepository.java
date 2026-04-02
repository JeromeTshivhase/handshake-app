package com.trustmarket.handshake.messaging.repository;

import com.trustmarket.handshake.messaging.entity.Message;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
        SELECT m FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.deletedAt IS NULL
        ORDER BY m.createdAt ASC
        """)
    Page<Message> findVisibleByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "conversation",
            "conversation.listing",
            "conversation.buyer",
            "conversation.seller",
            "sender"
    })
    Optional<Message> findDetailedById(UUID id);

    /**
     * Count unread messages efficiently — replaces the N+1 load-all-then-filter pattern.
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.read = false
          AND m.sender.id <> :userId
          AND m.deletedAt IS NULL
        """)
    long countUnread(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.read = true
        WHERE m.conversation.id = :conversationId
          AND m.sender.id <> :userId
          AND m.read = false
          AND m.deletedAt IS NULL
        """)
    int markAllAsReadByConversationAndUser(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.read = true
        WHERE m.id = :messageId
          AND m.conversation.id = :conversationId
          AND m.sender.id <> :userId
          AND m.read = false
        """)
    int markAsReadByIdAndUser(
            @Param("conversationId") UUID conversationId,
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId);
}
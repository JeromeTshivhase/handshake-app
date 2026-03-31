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
    Page<Message> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "conversation",
            "conversation.listing",
            "conversation.buyer",
            "conversation.seller",
            "sender"
    })
    Optional<Message> findDetailedById(UUID id);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.read = true
        WHERE m.conversation.id = :conversationId
          AND m.sender.id <> :userId
          AND m.read = false
    """)
    int markAllAsReadByConversationAndUser(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.read = true
        WHERE m.id = :messageId
          AND m.sender.id <> :userId
          AND m.read = false
    """)
    int markAsReadByIdAndUser(@Param("messageId") UUID messageId, @Param("userId") UUID userId);
}
package com.trustmarket.handshake.messaging.repository;

import com.trustmarket.handshake.messaging.entity.Message;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
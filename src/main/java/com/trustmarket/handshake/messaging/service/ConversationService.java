package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.messaging.dto.ConversationResponse;
import com.trustmarket.handshake.messaging.dto.InitiateConversationRequest;
import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.repository.ConversationRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public ConversationResponse startFromListing(UUID listingId, UUID buyerId, InitiateConversationRequest req) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("Buyer not found: " + buyerId));

        User seller = listing.getSeller();

        // Rule: seller cannot buy own listing
        if (seller.getId().equals(buyerId)) {
            throw new BadRequestException("You cannot message yourself about your own listing");
        }

        // conversation unique per listing + buyer + seller
        Conversation conversation = conversationRepository.findUnique(listingId, buyerId, seller.getId())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .listing(listing)
                                .buyer(buyer)
                                .seller(seller)
                                .build()
                ));

        // initial message
        messageService.send(conversation.getId(), buyerId, req.getInitialMessage(), null, req.getChannels());

        return ConversationResponse.fromEntity(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> myConversations(UUID userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return conversationRepository.findForUser(userId, pageable).map(ConversationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Conversation getOwnedConversation(UUID conversationId, UUID userId) {
        Conversation c = conversationRepository.findByIdWithDetails(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        if (!c.containsUser(userId)) throw new BadRequestException("Not a participant of this conversation");
        return c;
    }
}
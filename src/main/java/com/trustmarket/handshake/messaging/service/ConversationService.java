package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.messaging.dto.ConversationResponse;
import com.trustmarket.handshake.messaging.dto.InitiateConversationRequest;
import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.entity.ConversationStatus;
import com.trustmarket.handshake.messaging.repository.ConversationRepository;
import com.trustmarket.handshake.messaging.entity.UserBlock;
import com.trustmarket.handshake.messaging.repository.UserBlockRepository;
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
    private final UserBlockRepository userBlockRepository;
    private final MessageService messageService;

    @Transactional
    public ConversationResponse startFromListing(UUID listingId, UUID buyerId, InitiateConversationRequest req) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Cannot message about inactive listing");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("Buyer not found: " + buyerId));

        User seller = listing.getSeller();

        if (isBlockedEitherDirection(buyerId, seller.getId())) {
            throw new BadRequestException("Messaging is blocked between these users");
        }

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

        if (conversation.getStatus() == ConversationStatus.BLOCKED) {
            throw new BadRequestException("This conversation is blocked");
        }

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

    @Transactional
    public void archive(UUID conversationId, UUID userId) {
        Conversation c = getOwnedConversation(conversationId, userId);
        if (c.getStatus() == ConversationStatus.BLOCKED) {
            throw new BadRequestException("Blocked conversations cannot be archived");
        }
        c.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(c);
    }

    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BadRequestException("You cannot block yourself");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new NotFoundException("User not found: " + blockerId));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new NotFoundException("User not found: " + blockedId));

        if (!userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)) {
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build());
        }

        conversationRepository.findBetweenUsers(blockerId, blockedId).forEach(c -> c.setStatus(ConversationStatus.BLOCKED));
    }

    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        long removed = userBlockRepository.deleteByBlocker_IdAndBlocked_Id(blockerId, blockedId);
        if (removed == 0) {
            return;
        }

        // Only reactivate if there is no reverse-direction block still active.
        if (!userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockedId, blockerId)) {
            conversationRepository.findBetweenUsers(blockerId, blockedId)
                    .forEach(c -> {
                        if (c.getStatus() == ConversationStatus.BLOCKED) {
                            c.setStatus(ConversationStatus.ACTIVE);
                        }
                    });
        }
    }

    public boolean isBlockedEitherDirection(UUID userA, UUID userB) {
        return userBlockRepository.existsByBlocker_IdAndBlocked_Id(userA, userB)
                || userBlockRepository.existsByBlocker_IdAndBlocked_Id(userB, userA);
    }
}
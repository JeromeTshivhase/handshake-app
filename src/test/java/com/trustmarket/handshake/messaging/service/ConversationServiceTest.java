package com.trustmarket.handshake.messaging.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.messaging.dto.InitiateConversationRequest;
import com.trustmarket.handshake.messaging.entity.Conversation;
import com.trustmarket.handshake.messaging.entity.ConversationStatus;
import com.trustmarket.handshake.messaging.repository.ConversationRepository;
import com.trustmarket.handshake.messaging.repository.UserBlockRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private MessageService messageService;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                listingRepository,
                userRepository,
                userBlockRepository,
                messageService
        );
    }

    @Test
    void startFromListingThrowsWhenBlockedEitherDirection() {
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        User seller = User.builder().id(sellerId).email("seller@example.com").passwordHash("x").firstName("S").lastName("L").build();
        Listing listing = Listing.builder().id(listingId).seller(seller).status(ListingStatus.ACTIVE).title("Phone").build();
        User buyer = User.builder().id(buyerId).email("buyer@example.com").passwordHash("x").firstName("B").lastName("Y").build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(buyerId, sellerId)).thenReturn(false);
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(sellerId, buyerId)).thenReturn(true);

        InitiateConversationRequest req = InitiateConversationRequest.builder()
                .initialMessage("hi")
                .channels(Set.of())
                .build();

        assertThrows(BadRequestException.class, () ->
                conversationService.startFromListing(listingId, buyerId, req));

        verify(conversationRepository, never()).save(any());
        verify(messageService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void blockAndUnblockUpdateConversationStatus() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();

        User blocker = User.builder().id(blockerId).email("a@example.com").passwordHash("x").firstName("A").lastName("A").build();
        User blocked = User.builder().id(blockedId).email("b@example.com").passwordHash("x").firstName("B").lastName("B").build();

        Conversation c = Conversation.builder()
                .id(UUID.randomUUID())
                .buyer(blocker)
                .seller(blocked)
                .status(ConversationStatus.ACTIVE)
                .build();

        when(userRepository.findById(blockerId)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(blocked));
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(false);
        when(conversationRepository.findBetweenUsers(blockerId, blockedId)).thenReturn(List.of(c));

        conversationService.blockUser(blockerId, blockedId);

        assertEquals(ConversationStatus.BLOCKED, c.getStatus());
        verify(userBlockRepository).save(any());

        when(userBlockRepository.deleteByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(1L);
        when(userBlockRepository.existsByBlocker_IdAndBlocked_Id(blockedId, blockerId)).thenReturn(false);

        conversationService.unblockUser(blockerId, blockedId);

        assertEquals(ConversationStatus.ACTIVE, c.getStatus());
    }
}


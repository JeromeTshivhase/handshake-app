package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
import com.trustmarket.handshake.safe_meetup.repository.MeetupRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private HandshakeRepository handshakeRepository;

    @Mock
    private MeetupRepository meetupRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, listingRepository, notificationPublisher,
                handshakeRepository, meetupRepository);
    }

    @Test
    void completePublishesTransactionCompletedNotification() {
        UUID buyerId = UUID.randomUUID();
        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Laptop")
                .status(ListingStatus.ACTIVE)
                .build();

        User buyer = User.builder().id(buyerId).email("buyer@example.com").passwordHash("x").firstName("Buyer").lastName("One").build();
        User seller = User.builder().id(UUID.randomUUID()).email("seller@example.com").passwordHash("x").firstName("Seller").lastName("Two").build();

        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .amount(new BigDecimal("2200.00"))
                .status(TransactionStatus.ESCROW_HELD)
                .build();

        Handshake handshake = Handshake.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .listing(listing)
                .agreedPrice(new BigDecimal("2200.00"))
                .status(HandshakeStatus.ACCEPTED)
                .build();
        txn.setHandshake(handshake);

        when(transactionRepository.findByIdWithDetails(txn.getId())).thenReturn(Optional.of(txn));
        when(meetupRepository.findByHandshake_Id(handshake.getId())).thenReturn(Optional.empty());
        when(transactionRepository.save(txn)).thenReturn(txn);
        when(listingRepository.save(listing)).thenReturn(listing);

        Transaction completed = transactionService.complete(txn.getId(), buyerId);

        assertEquals(TransactionStatus.COMPLETED, completed.getStatus());
        assertEquals(ListingStatus.SOLD, listing.getStatus());
        verify(notificationPublisher).publishTransactionCompleted(txn);
    }

    @Test
    void completeFailsWhileEscrowHoldIsActive() {
        UUID buyerId = UUID.randomUUID();
        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Phone")
                .status(ListingStatus.ACTIVE)
                .build();

        User buyer = User.builder().id(buyerId).email("buyer@example.com").passwordHash("x").firstName("Buyer").lastName("One").build();
        User seller = User.builder().id(UUID.randomUUID()).email("seller@example.com").passwordHash("x").firstName("Seller").lastName("Two").build();

        Handshake handshake = Handshake.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .listing(listing)
                .agreedPrice(new BigDecimal("500.00"))
                .status(HandshakeStatus.ESCROW_HELD)
                .build();

        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .handshake(handshake)
                .buyer(buyer)
                .seller(seller)
                .amount(new BigDecimal("500.00"))
                .status(TransactionStatus.ESCROW_HELD)
                .escrowReleaseAt(LocalDateTime.now().plusHours(12))
                .build();

        when(transactionRepository.findByIdWithDetails(txn.getId())).thenReturn(Optional.of(txn));

        assertThrows(BadRequestException.class, () -> transactionService.complete(txn.getId(), buyerId));
        verifyNoInteractions(meetupRepository, listingRepository, handshakeRepository, notificationPublisher);
    }

    @Test
    void autoReleaseEscrowIfDueCompletesTransaction() {
        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Camera")
                .status(ListingStatus.ACTIVE)
                .build();

        User buyer = User.builder().id(UUID.randomUUID()).email("buyer@example.com").passwordHash("x").firstName("Buyer").lastName("One").build();
        User seller = User.builder().id(UUID.randomUUID()).email("seller@example.com").passwordHash("x").firstName("Seller").lastName("Two").build();

        Handshake handshake = Handshake.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .listing(listing)
                .agreedPrice(new BigDecimal("1200.00"))
                .status(HandshakeStatus.ESCROW_HELD)
                .build();

        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .handshake(handshake)
                .buyer(buyer)
                .seller(seller)
                .amount(new BigDecimal("1200.00"))
                .status(TransactionStatus.ESCROW_HELD)
                .escrowReleaseAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(transactionRepository.findByIdWithDetails(txn.getId())).thenReturn(Optional.of(txn));
        when(meetupRepository.findByHandshake_Id(handshake.getId())).thenReturn(Optional.empty());
        when(transactionRepository.save(txn)).thenReturn(txn);
        when(listingRepository.save(listing)).thenReturn(listing);

        boolean released = transactionService.autoReleaseEscrowIfDue(txn.getId());

        assertTrue(released);
        assertEquals(TransactionStatus.COMPLETED, txn.getStatus());
        assertEquals(HandshakeStatus.COMPLETED, handshake.getStatus());
        assertEquals(ListingStatus.SOLD, listing.getStatus());
        verify(notificationPublisher).publishTransactionCompleted(txn);
        verify(notificationPublisher).publishEscrowReleased(txn);
    }

    @Test
    void autoReleaseEscrowIfDueSkipsWhenMeetupNotCompleted() {
        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Monitor")
                .status(ListingStatus.ACTIVE)
                .build();

        User buyer = User.builder().id(UUID.randomUUID()).email("buyer@example.com").passwordHash("x").firstName("Buyer").lastName("One").build();
        User seller = User.builder().id(UUID.randomUUID()).email("seller@example.com").passwordHash("x").firstName("Seller").lastName("Two").build();

        Handshake handshake = Handshake.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .listing(listing)
                .agreedPrice(new BigDecimal("900.00"))
                .status(HandshakeStatus.ESCROW_HELD)
                .build();

        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .handshake(handshake)
                .buyer(buyer)
                .seller(seller)
                .amount(new BigDecimal("900.00"))
                .status(TransactionStatus.ESCROW_HELD)
                .escrowReleaseAt(LocalDateTime.now().minusMinutes(1))
                .build();

        Meetup meetup = Meetup.builder()
                .id(UUID.randomUUID())
                .handshake(handshake)
                .status(MeetupStatus.ACTIVE)
                .build();

        when(transactionRepository.findByIdWithDetails(txn.getId())).thenReturn(Optional.of(txn));
        when(meetupRepository.findByHandshake_Id(handshake.getId())).thenReturn(Optional.of(meetup));

        boolean released = transactionService.autoReleaseEscrowIfDue(txn.getId());

        assertFalse(released);
        assertEquals(TransactionStatus.ESCROW_HELD, txn.getStatus());
        verifyNoInteractions(listingRepository, handshakeRepository, notificationPublisher);
    }
}

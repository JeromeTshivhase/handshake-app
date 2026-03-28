package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.listing.entity.ListingStatus;
import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
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

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, listingRepository, notificationPublisher, handshakeRepository);
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
        when(transactionRepository.save(txn)).thenReturn(txn);
        when(listingRepository.save(listing)).thenReturn(listing);

        Transaction completed = transactionService.complete(txn.getId(), buyerId);

        assertEquals(TransactionStatus.COMPLETED, completed.getStatus());
        assertEquals(ListingStatus.SOLD, listing.getStatus());
        verify(notificationPublisher).publishTransactionCompleted(txn);
    }
}

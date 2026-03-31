package com.trustmarket.handshake.shared.security;

import com.trustmarket.handshake.listing.repository.ListingRepository;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.messaging.repository.MessageRepository;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnershipService {
    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final MessageRepository messageRepository;

    public void assertListingOwner(UUID listingId, UUID userId) {
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ForbiddenException("Listing not found or not accessible"));
        if (!listing.getSeller().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this listing");
        }
    }

    public void assertTransactionOwner(UUID transactionId, UUID userId) {
        var tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ForbiddenException("Transaction not found or not accessible"));
        if (!tx.getBuyer().getId().equals(userId) && !tx.getSeller().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this transaction");
        }
    }

    public void assertMessageOwner(UUID messageId, UUID userId) {
        var msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ForbiddenException("Message not found or not accessible"));
        if (!msg.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this message");
        }
    }
}

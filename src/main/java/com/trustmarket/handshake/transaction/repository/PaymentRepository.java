package com.trustmarket.handshake.transaction.repository;

import com.trustmarket.handshake.transaction.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findTopByTransaction_IdOrderByCreatedAtDesc(UUID transactionId);
}
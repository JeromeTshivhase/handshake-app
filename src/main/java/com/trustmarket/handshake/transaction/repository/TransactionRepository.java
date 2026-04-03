package com.trustmarket.handshake.transaction.repository;

import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @EntityGraph(attributePaths = {"listing", "buyer", "seller", "handshake"})
    Optional<Transaction> findByHandshake_Id(UUID handshakeId);

    @EntityGraph(attributePaths = {"listing", "buyer", "seller", "handshake"})
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.buyer.id = :userId OR t.seller.id = :userId) AND t.status = :status")
    long countByUserAndStatus(@Param("userId") UUID userId, @Param("status") TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE (t.buyer.id = :userId OR t.seller.id = :userId) AND t.status = :status")
    BigDecimal sumAmountByUserAndStatus(@Param("userId") UUID userId, @Param("status") TransactionStatus status);

    List<Transaction> findAllByStatusAndEscrowReleaseAtLessThanEqual(TransactionStatus status, LocalDateTime cutoff);
}
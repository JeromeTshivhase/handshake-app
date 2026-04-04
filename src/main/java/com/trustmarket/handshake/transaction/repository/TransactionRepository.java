package com.trustmarket.handshake.transaction.repository;

import com.trustmarket.handshake.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @EntityGraph(attributePaths = {"listing", "buyer", "seller", "handshake"})
    Optional<Transaction> findByHandshake_Id(UUID handshakeId);

    @EntityGraph(attributePaths = {"listing", "buyer", "seller", "handshake"})
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithDetails(@Param("id") UUID id);

    // For trust score calculations
    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.buyer.id = :userId OR t.seller.id = :userId) AND t.status = :status")
    long countByUserAndStatus(@Param("userId") UUID userId, @Param("status") com.trustmarket.handshake.transaction.enums.TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE (t.buyer.id = :userId OR t.seller.id = :userId) AND t.status = :status")
    java.math.BigDecimal sumAmountByUserAndStatus(@Param("userId") UUID userId, @Param("status") com.trustmarket.handshake.transaction.enums.TransactionStatus status);

    // Find escrow-held transactions where hold expired or release time is due
    @Query("SELECT t FROM Transaction t WHERE t.status = com.trustmarket.handshake.transaction.enums.TransactionStatus.ESCROW_HELD AND t.escrowReleaseAt <= :now")
    java.util.List<Transaction> findAllByStatusAndEscrowReleaseAtLessThanEqual(@Param("now") java.time.LocalDateTime now);
}
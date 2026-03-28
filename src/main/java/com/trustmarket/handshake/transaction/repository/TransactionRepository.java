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
}
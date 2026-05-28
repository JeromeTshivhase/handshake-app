package com.trustmarket.handshake.user.repository;

import com.trustmarket.handshake.user.entity.PayoutMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayoutMethodRepository extends JpaRepository<PayoutMethod, UUID> {
    Optional<PayoutMethod> findByUser_Id(UUID userId);
    boolean existsByAccountNumberHash(String accountNumberHash);
    Optional<PayoutMethod> findByAccountNumberHash(String accountNumberHash);
}


package com.trustmarket.handshake.transaction.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.transaction.dto.TransactionResponse;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.repository.PaymentRepository;
import com.trustmarket.handshake.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(Authentication auth, @PathVariable UUID id) {
        UUID userId = AuthUtils.userId(auth);
        Transaction txn = transactionService.get(id, userId);
        return ApiResponse.ok(buildResponse(txn));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<TransactionResponse> complete(Authentication auth, @PathVariable UUID id) {
        UUID buyerId = AuthUtils.userId(auth);
        Transaction txn = transactionService.complete(id, buyerId);
        return ApiResponse.ok(buildResponse(txn));
    }

    private TransactionResponse buildResponse(Transaction txn) {
        return paymentRepository
                .findTopByTransaction_IdOrderByCreatedAtDesc(txn.getId())
                .map(p -> TransactionResponse.from(txn, p.getStatus(), p.getPaymentReference()))
                .orElseGet(() -> TransactionResponse.from(txn));
    }
}
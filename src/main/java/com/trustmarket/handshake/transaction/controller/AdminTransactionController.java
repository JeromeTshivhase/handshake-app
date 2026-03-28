package com.trustmarket.handshake.transaction.controller;

import com.trustmarket.handshake.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {
    private final TransactionService transactionService;

    @PostMapping("/{transactionId}/dispute")
    public ResponseEntity<?> markDisputed(@PathVariable UUID transactionId, @RequestParam(required = false) String reason) {
        transactionService.markDisputed(transactionId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<?> refundToBuyer(@PathVariable UUID transactionId) {
        transactionService.refundToBuyer(transactionId);
        return ResponseEntity.ok().build();
    }
}


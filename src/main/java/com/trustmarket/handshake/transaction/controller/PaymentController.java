package com.trustmarket.handshake.transaction.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.ForbiddenException;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.transaction.dto.PaymentInitiateRequest;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.transaction.service.PayFastService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionRepository transactionRepository;
    private final PayFastService payFastService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a payment", operationId = "initiatePayment")
    public ApiResponse<Map<String, String>> initiate(Authentication auth, @Valid @RequestBody PaymentInitiateRequest req) {
        UUID buyerId = AuthUtils.userId(auth);

        Transaction txn = transactionRepository.findByHandshake_Id(req.getHandshakeId())
                .orElseThrow(() -> new BadRequestException("Transaction not found for handshake"));

        if (!txn.getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenException("Only buyer can initiate payment");
        }

        String url = payFastService.initiatePayFast(txn, req.getReturnUrl());
        return ApiResponse.ok(Map.of(
                "paymentUrl", url,
                "transactionId", txn.getId().toString()
        ));
    }

    /**
     * PayFast ITN callback (PUBLIC endpoint)
     * Must be permitAll() in SecurityConfig.
     */
    @PostMapping(value = "/payfast/notify", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "PayFast ITN webhook callback. Receives payment notifications from PayFast.", operationId = "payfastNotifyWebhook")
    public ResponseEntity<String> payfastNotify(@RequestBody(required = false) String rawBody) {
        payFastService.processItn(rawBody);
        return ResponseEntity.ok("OK");
    }
}
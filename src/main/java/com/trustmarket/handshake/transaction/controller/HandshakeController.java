package com.trustmarket.handshake.transaction.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.fraud.FraudDetectionService;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.transaction.dto.CreateHandshakeRequest;
import com.trustmarket.handshake.transaction.dto.HandshakeResponse;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.enums.HandshakeStatus;
import com.trustmarket.handshake.transaction.enums.TransactionStatus;
import com.trustmarket.handshake.transaction.service.HandshakeService;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/handshakes")
@RequiredArgsConstructor
public class HandshakeController {
    private final HandshakeService handshakeService;
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get my handshakes", operationId = "getMyHandshakes")
    public ApiResponse<Page<HandshakeResponse>> myHandshakes(
            Authentication auth,
            @RequestParam(required = false) HandshakeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        UUID userId = AuthUtils.userId(auth);
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        Page<Handshake> handshakes = handshakeService.findByUserIdAndStatus(userId, status, pageable);
        Page<HandshakeResponse> responsePage = handshakes.map(h -> {
            var txnOpt = transactionRepository.findByHandshake_Id(h.getId());
            TransactionStatus txnStatus = txnOpt.map(txn -> txn.getStatus()).orElse(null);
            UUID txnId = txnOpt.map(txn -> txn.getId()).orElse(null);
            return HandshakeResponse.from(h, txnStatus, txnId);
        });
        return ApiResponse.ok(responsePage);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create handshake", operationId = "createHandshake")
    public ApiResponse<HandshakeResponse> create(Authentication auth, @Valid @RequestBody CreateHandshakeRequest req) {
        UUID buyerId = AuthUtils.userId(auth);
        fraudDetectionService.checkFraud(buyerId, "handshake_create");
        Handshake h = handshakeService.create(buyerId, req);
        return ApiResponse.ok(HandshakeResponse.from(h));
    }

    @PostMapping(value = "/{id}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Accept handshake", operationId = "acceptHandshake")
    public ApiResponse<HandshakeResponse> accept(Authentication auth, @PathVariable UUID id) {
        UUID sellerId = AuthUtils.userId(auth);
        fraudDetectionService.checkFraud(sellerId, "handshake_accept");
        Handshake h = handshakeService.accept(sellerId, id);
        return ApiResponse.ok(HandshakeResponse.from(h));
    }

    @PostMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reject handshake", operationId = "rejectHandshake")
    public ApiResponse<HandshakeResponse> reject(Authentication auth, @PathVariable UUID id) {
        UUID sellerId = AuthUtils.userId(auth);
        Handshake h = handshakeService.reject(sellerId, id);
        return ApiResponse.ok(HandshakeResponse.from(h));
    }

    @GetMapping("/{id}")
    public ApiResponse<HandshakeResponse> get(Authentication auth, @PathVariable UUID id) {
        UUID userId = AuthUtils.userId(auth);
        Handshake h = handshakeService.get(id, userId);
        var txnOpt = transactionRepository.findByHandshake_Id(h.getId());
        TransactionStatus txnStatus = txnOpt.map(txn -> txn.getStatus()).orElse(null);
        UUID txnId = txnOpt.map(txn -> txn.getId()).orElse(null);
        return ApiResponse.ok(HandshakeResponse.from(h, txnStatus, txnId));
    }

    @GetMapping("/seller/me")
    public ApiResponse<Page<HandshakeResponse>> sellerHandshakes(
            Authentication auth,
            @RequestParam(required = false) HandshakeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        UUID sellerId = AuthUtils.userId(auth);
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        Page<Handshake> handshakes =
                handshakeService.findBySellerIdAndStatus(sellerId, status, pageable);
        Page<HandshakeResponse> responsePage = handshakes.map(h -> {
            var txnOpt = transactionRepository.findByHandshake_Id(h.getId());
            TransactionStatus txnStatus = txnOpt.map(txn -> txn.getStatus()).orElse(null);
            UUID txnId = txnOpt.map(txn -> txn.getId()).orElse(null);
            return HandshakeResponse.from(h, txnStatus, txnId);
        });
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/buyer/me")
    public ApiResponse<Page<HandshakeResponse>> buyerHandshakes(
            Authentication auth,
            @RequestParam(required = false) HandshakeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        UUID buyerId = AuthUtils.userId(auth);
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        Page<Handshake> handshakes =
                handshakeService.findByBuyerIdAndStatus(buyerId, status, pageable);
        Page<HandshakeResponse> responsePage = handshakes.map(h -> {
            var txnOpt = transactionRepository.findByHandshake_Id(h.getId());
            TransactionStatus txnStatus = txnOpt.map(txn -> txn.getStatus()).orElse(null);
            UUID txnId = txnOpt.map(txn -> txn.getId()).orElse(null);
            return HandshakeResponse.from(h, txnStatus, txnId);
        });
        return ApiResponse.ok(responsePage);
    }
}
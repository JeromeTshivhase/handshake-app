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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
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
     * PayFast ITN callback (PUBLIC endpoint — no auth, no CORS via dedicated SecurityFilterChain).
     * PayFast POSTs application/x-www-form-urlencoded data.  We read the raw body from the
     * InputStream; if the servlet container already consumed it for parameter parsing we fall
     * back to reconstructing the query-string from the parsed parameter map.
     */
    @PostMapping(value = "/payfast/notify")
    @Operation(summary = "PayFast ITN webhook callback. Receives payment notifications from PayFast.", operationId = "payfastNotifyWebhook")
    public ResponseEntity<String> payfastNotify(HttpServletRequest request) {
        String rawBody = readRawBody(request);
        log.info("PayFast ITN received. remoteAddr={} body={}", request.getRemoteAddr(), rawBody);
        payFastService.processItn(rawBody);
        return ResponseEntity.ok("OK");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String readRawBody(HttpServletRequest request) {
        // 1. Try reading the raw InputStream (works when body has not yet been consumed).
        try {
            byte[] bytes = request.getInputStream().readAllBytes();
            if (bytes.length > 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }

        // 2. Fallback: body was already consumed by the servlet container parsing form params.
        //    Reconstruct the URL-encoded string from the parsed parameter map so that
        //    PayFastService.parseFormEncodedBody() can still decode it correctly.
        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            for (String val : entry.getValue()) {
                if (!sb.isEmpty()) sb.append('&');
                sb.append(key).append('=').append(URLEncoder.encode(val, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}


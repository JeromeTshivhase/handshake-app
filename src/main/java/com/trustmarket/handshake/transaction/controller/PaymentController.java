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
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionRepository transactionRepository;
    private final PayFastService payFastService;

    /**
     * Comma-separated list of CIDR blocks / individual IPs that PayFast POSTs from.
     * Override in env with PAYFAST_ALLOWED_IPS if ranges change.
     */
    @Value("${payfast.allowed-ips:197.97.145.144/28,196.33.227.224/28,41.74.179.192/27}")
    private String payfastAllowedIpsCfg;

    @Value("${payfast.ip-check-enabled:true}")
    private boolean ipCheckEnabled;

    @Value("${app.allowed-return-url-hosts:}")
    private String allowedReturnUrlHostsCfg;

    private List<CidrRange> payfastCidrs;
    private Set<String> allowedReturnUrlHosts;

    @PostConstruct
    void init() {
        payfastCidrs = Arrays.stream(payfastAllowedIpsCfg.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(CidrRange::parse)
                .collect(Collectors.toList());

        allowedReturnUrlHosts = Arrays.stream(allowedReturnUrlHostsCfg.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a payment", operationId = "initiatePayment")
    public ApiResponse<Map<String, String>> initiate(
            Authentication auth,
            @Valid @RequestBody PaymentInitiateRequest req
    ) {
        UUID buyerId = AuthUtils.userId(auth);

        if (!allowedReturnUrlHosts.isEmpty()) {
            validateReturnUrl(req.getReturnUrl());
        }

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
    @Operation(summary = "PayFast ITN webhook callback", operationId = "payfastNotifyWebhook")
    public ResponseEntity<String> payfastNotify(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (ipCheckEnabled && !isPayFastIp(remoteAddr)) {
            log.warn("PayFast ITN rejected: untrusted source IP={}", remoteAddr);
            return ResponseEntity.status(403).body("Forbidden");
        }

        String rawBody = readRawBody(request);
        log.info("PayFast ITN received. remoteAddr={} bytes={}", remoteAddr, rawBody == null ? 0 : rawBody.length());
        payFastService.processItn(rawBody);
        return ResponseEntity.ok("OK");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void validateReturnUrl(String returnUrl) {
        try {
            URI uri = new URI(returnUrl);
            String host = uri.getHost();
            if (host == null) throw new BadRequestException("returnUrl has no host");
            boolean allowed = allowedReturnUrlHosts.stream()
                    .anyMatch(h -> host.equals(h) || host.endsWith("." + h));
            if (!allowed) {
                throw new BadRequestException("returnUrl host is not allowed: " + host);
            }
        } catch (java.net.URISyntaxException e) {
            throw new BadRequestException("Invalid returnUrl: " + e.getMessage());
        }
    }

    private boolean isPayFastIp(String remoteAddr) {
        try {
            InetAddress addr = InetAddress.getByName(remoteAddr);
            return payfastCidrs.stream().anyMatch(cidr -> cidr.contains(addr));
        } catch (Exception e) {
            log.warn("Could not parse remote address '{}': {}", remoteAddr, e.getMessage());
            return false;
        }
    }

    private String readRawBody(HttpServletRequest request) {
        try {
            byte[] bytes = request.getInputStream().readAllBytes();
            if (bytes.length > 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }

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

    // ── CIDR helper ──────────────────────────────────────────────────────────

    private record CidrRange(byte[] networkAddress, int prefixLength) {

        static CidrRange parse(String cidr) {
            if (!cidr.contains("/")) {
                // Single IP — treat as /32
                return parse(cidr + "/32");
            }
            String[] parts = cidr.split("/");
            try {
                byte[] addr = InetAddress.getByName(parts[0]).getAddress();
                int prefix = Integer.parseInt(parts[1]);
                return new CidrRange(addr, prefix);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid CIDR: " + cidr, e);
            }
        }

        boolean contains(InetAddress candidate) {
            byte[] candidateBytes = candidate.getAddress();
            if (candidateBytes.length != networkAddress.length) return false;

            int fullBytes = prefixLength / 8;
            int remainderBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (networkAddress[i] != candidateBytes[i]) return false;
            }
            if (remainderBits > 0) {
                int mask = 0xFF << (8 - remainderBits) & 0xFF;
                return (networkAddress[fullBytes] & mask) == (candidateBytes[fullBytes] & mask);
            }
            return true;
        }
    }
}

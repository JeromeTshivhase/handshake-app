package com.trustmarket.handshake.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.transaction.entity.Payment;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.PaymentProvider;
import com.trustmarket.handshake.transaction.enums.PaymentStatus;
import com.trustmarket.handshake.transaction.repository.HandshakeRepository;
import com.trustmarket.handshake.transaction.repository.PaymentRepository;
import com.trustmarket.handshake.safe_meetup.service.SafeMeetupOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayFastService {

    private final PaymentRepository paymentRepository;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;
    private final NotificationPublisher notificationPublisher;
    private final SafeMeetupOtpService safeMeetupOtpService;
    private final HandshakeRepository handshakeRepository;

    @Value("${payfast.merchant-id}") private String merchantId;
    @Value("${payfast.merchant-key}") private String merchantKey;
    @Value("${payfast.passphrase:}") private String passphrase;
    @Value("${payfast.cancel-url}") private String cancelUrl;
    @Value("${payfast.notify-url}") private String notifyUrl;
    @Value("${payfast.mode:sandbox}") private String mode;

    private static final String SANDBOX = "https://sandbox.payfast.co.za/eng/process";
    private static final String LIVE = "https://www.payfast.co.za/eng/process";

    @Transactional
    public String initiatePayFast(Transaction txn, String returnUrl) {
        // Create payment record
        Payment payment = Payment.builder()
                .transaction(txn)
                .amount(txn.getAmount())
                .provider(PaymentProvider.PAYFAST)
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        // Mark txn as payment pending
        transactionService.markPaymentPending(txn);
        notificationPublisher.publishPaymentInitiated(payment);

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", merchantId);
        params.put("merchant_key", merchantKey);
        params.put("return_url", returnUrl);
        params.put("cancel_url", cancelUrl);
        params.put("notify_url", notifyUrl);

        // PayFast m_payment_id should be our internal payment id OR transaction id.
        // We use PAYMENT id so we can uniquely map ITN to payment record.
        params.put("m_payment_id", payment.getId().toString());

        params.put("amount", txn.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("item_name", "Handshake-" + txn.getHandshake().getId().toString().substring(0, 8));
        params.put("item_description", txn.getListing().getTitle());

        // custom fields
        params.put("custom_str1", txn.getHandshake().getId().toString());
        params.put("custom_str2", txn.getId().toString());

        String signature = sign(params);
        params.put("signature", signature);

        return buildRedirectUrl(params);
    }

    /**
     * ITN payload comes as form-url-encoded. We receive it as Map.
     * Expected fields: m_payment_id, payment_status, pf_payment_id, signature, etc.
     */
    @Transactional
    public void processItn(String rawBody) {
        LinkedHashMap<String, String> payload = parseFormEncodedBody(rawBody);

        String paymentId = payload.get("m_payment_id");
        String paymentStatus = payload.get("payment_status");
        String pfPaymentId = payload.get("pf_payment_id");

        if (paymentId == null || paymentStatus == null) {
            throw new BadRequestException("Missing required ITN fields");
        }

        String receivedSig = payload.get("signature");
        if (receivedSig != null && !receivedSig.isBlank() && !verifySignature(rawBody, receivedSig)) {
            log.warn("PayFast ITN signature mismatch for payment {}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new BadRequestException("Payment not found: " + paymentId));

        // store raw payload for audit
        try {
            payment.setRawPayload(rawBody != null && !rawBody.isBlank()
                    ? rawBody
                    : objectMapper.writeValueAsString(payload));
        } catch (Exception ignore) {}

        if ("COMPLETE".equalsIgnoreCase(paymentStatus)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentReference(pfPaymentId);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // move transaction to ESCROW_HELD
            Transaction txn = payment.getTransaction();
            transactionService.markEscrowHeld(txn);
            notificationPublisher.publishPaymentPaid(payment);

            // --- Set handshake status to ESCROW_HELD and save ---
            var handshake = txn.getHandshake();
            if (handshake.getStatus() != com.trustmarket.handshake.transaction.enums.HandshakeStatus.ESCROW_HELD) {
                handshake.setStatus(com.trustmarket.handshake.transaction.enums.HandshakeStatus.ESCROW_HELD);
                handshakeRepository.save(handshake);
            }

            log.info("Payment PAID and escrow held. payment={}, txn={}", payment.getId(), txn.getId());

            // --- Trigger OTP generation if eligible ---
            try {
                // Only enable OTP if handshake is ESCROW_HELD and transaction is ESCROW_HELD
                if (handshake.getStatus() == com.trustmarket.handshake.transaction.enums.HandshakeStatus.ESCROW_HELD
                        && txn.getStatus() == com.trustmarket.handshake.transaction.enums.TransactionStatus.ESCROW_HELD) {
                    // Use dummy coordinates (0,0) if not available; service will validate eligibility
                    safeMeetupOtpService.enableIfEligible(handshake.getId(), handshake.getBuyer().getId(), 0.0, 0.0);
                    log.info("OTP generation triggered for handshake {} after escrow held.", handshake.getId());
                }
            } catch (Exception e) {
                log.error("Failed to trigger OTP generation after escrow held: {}", e.getMessage(), e);
            }
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentReference(pfPaymentId);
            paymentRepository.save(payment);
            notificationPublisher.publishPaymentFailed(payment);
            log.info("Payment FAILED. payment={}", payment.getId());
        } else {
            log.info("ITN status ignored: {} for payment {}", paymentStatus, paymentId);
        }
    }

    // -------- signing helpers --------

    private String buildRedirectUrl(Map<String, String> params) {
        String base = "sandbox".equalsIgnoreCase(mode) ? SANDBOX : LIVE;

        StringBuilder qs = new StringBuilder();
        boolean first = true;
        for (var e : params.entrySet()) {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) continue;
            if (!first) qs.append("&");
            qs.append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue().trim()));
            first = false;
        }
        return base + "?" + qs;
    }

    private String sign(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (var e : params.entrySet()) {
            if (e.getKey() == null || "signature".equalsIgnoreCase(e.getKey())) continue;
            if (e.getValue() == null) continue;
            String v = e.getValue().trim();
            if (v.isEmpty()) continue;

            sb.append(e.getKey()).append("=").append(urlEncode(v)).append("&");
        }
        sb.setLength(sb.length() - 1);

        String baseString = sb.toString();
        if (passphrase != null && !passphrase.isBlank()) {
            baseString += "&passphrase=" + urlEncode(passphrase.trim());
        }
        return md5(baseString);
    }


    boolean verifySignature(String rawBody, String receivedSignature) {
        String baseString = buildVerificationBaseString(rawBody);

        if (baseString.isBlank()) {
            return false;
        }

        if (passphrase != null && !passphrase.isBlank()) {
            baseString += "&passphrase=" + urlEncode(passphrase.trim());
        }

        String calculated = md5(baseString);

        log.info("PayFast ITN verify: calculated={}, received={}", calculated, receivedSignature);

        return calculated.equalsIgnoreCase(receivedSignature);
    }

    LinkedHashMap<String, String> parseFormEncodedBody(String rawBody) {
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        if (rawBody == null || rawBody.isBlank()) {
            return payload;
        }

        for (String pair : rawBody.split("&")) {
            if (pair.isEmpty()) continue;

            int idx = pair.indexOf('=');
            String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
            String rawValue = idx >= 0 ? pair.substring(idx + 1) : "";

            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            payload.put(key, value);
        }

        return payload;
    }

    String buildVerificationBaseString(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String pair : rawBody.split("&")) {
            if (pair.isBlank()) continue;

            int idx = pair.indexOf('=');
            String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);

            if ("signature".equalsIgnoreCase(key)) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(pair);
        }

        return sb.toString();
    }

    private String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 failed", e);
        }
    }
}

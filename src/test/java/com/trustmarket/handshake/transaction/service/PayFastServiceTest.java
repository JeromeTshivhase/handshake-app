package com.trustmarket.handshake.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustmarket.handshake.listing.entity.Listing;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.entity.Payment;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.enums.PaymentProvider;
import com.trustmarket.handshake.transaction.enums.PaymentStatus;
import com.trustmarket.handshake.transaction.repository.PaymentRepository;
import com.trustmarket.handshake.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayFastServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private com.trustmarket.handshake.transaction.repository.HandshakeRepository handshakeRepository;

    private PayFastService payFastService;

    @BeforeEach
    void setUp() {
        payFastService = new PayFastService(
            paymentRepository,
            transactionService,
            new ObjectMapper(),
            notificationPublisher,
            handshakeRepository
        );
        ReflectionTestUtils.setField(payFastService, "passphrase", "");
        ReflectionTestUtils.setField(payFastService, "merchantId", "1000");
        ReflectionTestUtils.setField(payFastService, "merchantKey", "key");
        ReflectionTestUtils.setField(payFastService, "cancelUrl", "https://cancel.example");
        ReflectionTestUtils.setField(payFastService, "notifyUrl", "https://notify.example");
        ReflectionTestUtils.setField(payFastService, "mode", "sandbox");
    }

    @Test
    void parseFormEncodedBodyDecodesValuesAndPreservesOrder() {
        String rawBody = "m_payment_id=pay-1&item_name=Phone+Case&custom_str1=abc%2B123";

        LinkedHashMap<String, String> payload = payFastService.parseFormEncodedBody(rawBody);

        assertEquals(List.of("m_payment_id", "item_name", "custom_str1"), new ArrayList<>(payload.keySet()));
        assertEquals("pay-1", payload.get("m_payment_id"));
        assertEquals("Phone Case", payload.get("item_name"));
        assertEquals("abc+123", payload.get("custom_str1"));
    }

    @Test
    void buildVerificationBaseStringRemovesSignatureAndKeepsOriginalEncoding() {
        String rawBody = "m_payment_id=pay-1&payment_status=COMPLETE&item_description=Phone+Case&custom_str1=abc%2B123&signature=deadbeef";

        String baseString = payFastService.buildVerificationBaseString(rawBody);

        assertEquals("m_payment_id=pay-1&payment_status=COMPLETE&item_description=Phone+Case&custom_str1=abc%2B123", baseString);
    }

    @Test
    void verifySignatureUsesRawBodyAndPassphrase() {
        ReflectionTestUtils.setField(payFastService, "passphrase", "test pass");
        String base = "m_payment_id=pay-1&payment_status=COMPLETE&item_description=Phone+Case";
        String expectedSignature = md5(base + "&passphrase=test+pass");
        String rawBody = base + "&signature=" + expectedSignature;

        assertTrue(payFastService.verifySignature(rawBody, expectedSignature));
    }

    @Test
    void processItnReturnsEarlyOnSignatureMismatch() {
        String rawBody = "m_payment_id=0cca1ce3-6eb0-4030-8d9e-66524cfce315&payment_status=COMPLETE&signature=invalid";

        payFastService.processItn(rawBody);

        verifyNoInteractions(paymentRepository, transactionService, notificationPublisher);
    }

    @Test
    void initiatePayFastPublishesPaymentInitiatedNotification() {
        Transaction txn = transaction();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        String url = payFastService.initiatePayFast(txn, "https://return.example");

        assertTrue(url.contains("m_payment_id="));
        verify(transactionService).markPaymentPending(txn);
        verify(notificationPublisher).publishPaymentInitiated(any(Payment.class));
    }

    @Test
    void processItnPublishesPaymentPaidNotification() {
        Transaction txn = transaction();
        Payment payment = Payment.builder()
                .id(UUID.fromString("0cca1ce3-6eb0-4030-8d9e-66524cfce315"))
                .transaction(txn)
                .amount(txn.getAmount())
                .provider(PaymentProvider.PAYFAST)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        payFastService.processItn(signedItn(payment.getId(), "COMPLETE", "1500.00", "1000"));

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verify(transactionService).markEscrowHeld(txn);
        verify(notificationPublisher).publishPaymentPaid(payment);
    }

    private String signedItn(UUID paymentId, String status, String amount, String merchant) {
        String base = "m_payment_id=" + paymentId + "&pf_payment_id=pf-123&payment_status=" + status
                + "&amount_gross=" + amount + "&merchant_id=" + merchant;
        return base + "&signature=" + md5(base);
    }

    private Payment pendingPayment() {
        Transaction txn = transaction();
        return Payment.builder()
                .id(UUID.randomUUID())
                .transaction(txn)
                .amount(txn.getAmount())
                .provider(PaymentProvider.PAYFAST)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void processItnRejectsMissingSignature() {
        Payment payment = pendingPayment();

        payFastService.processItn("m_payment_id=" + payment.getId() + "&payment_status=COMPLETE&amount_gross=1500.00&merchant_id=1000");

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(paymentRepository, transactionService, notificationPublisher);
    }

    @Test
    void processItnRejectsWrongAmount() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        payFastService.processItn(signedItn(payment.getId(), "COMPLETE", "1.00", "1000"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(transactionService, notificationPublisher);
    }

    @Test
    void processItnRejectsWrongMerchant() {
        Payment payment = pendingPayment();

        payFastService.processItn(signedItn(payment.getId(), "COMPLETE", "1500.00", "9999"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(paymentRepository, transactionService, notificationPublisher);
    }

    @Test
    void processItnRefusesLiveModeWithoutPassphrase() {
        ReflectionTestUtils.setField(payFastService, "mode", "live");
        Payment payment = pendingPayment();

        payFastService.processItn(signedItn(payment.getId(), "COMPLETE", "1500.00", "1000"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(paymentRepository, transactionService, notificationPublisher);
    }

    @Test
    void processItnRejectsWhenPayFastDoesNotConfirm() {
        PayFastService spy = org.mockito.Mockito.spy(payFastService);
        ReflectionTestUtils.setField(spy, "serverValidationEnabled", true);
        org.mockito.Mockito.doReturn(false).when(spy).validateWithPayFast(any());
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        spy.processItn(signedItn(payment.getId(), "COMPLETE", "1500.00", "1000"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(transactionService, notificationPublisher);
    }

    private Transaction transaction() {
        Listing listing = Listing.builder()
                .id(UUID.randomUUID())
                .title("Phone")
                .build();

        Handshake handshake = Handshake.builder()
                .id(UUID.randomUUID())
                .build();

        User buyer = User.builder().id(UUID.randomUUID()).email("buyer@example.com").passwordHash("x").firstName("Buyer").lastName("One").build();
        User seller = User.builder().id(UUID.randomUUID()).email("seller@example.com").passwordHash("x").firstName("Seller").lastName("Two").build();

        return Transaction.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .handshake(handshake)
                .buyer(buyer)
                .seller(seller)
                .amount(new BigDecimal("1500.00"))
                .build();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.trustmarket.handshake.notification.service;

import com.trustmarket.handshake.notification.service.DomainNotificationEvent;
import com.trustmarket.handshake.notification.entity.NotificationEventType;
import com.trustmarket.handshake.transaction.entity.Payment;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishUserCreated(User user) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                user.getId(),
                NotificationEventType.USER_CREATED,
                "Welcome to Handshake",
                "Your account has been created successfully.",
                payloadOf(
                        "userId", user.getId().toString(),
                        "email", user.getEmail()
                ),
                true
        ));
    }

    public void publishPaymentInitiated(Payment payment) {
        Transaction txn = payment.getTransaction();
        String amount = txn.getAmount() == null ? null : txn.getAmount().toPlainString();
        Map<String, Object> payload = paymentPayload(payment);

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getBuyer().getId(),
                NotificationEventType.PAYMENT_INITIATED,
                "Payment started",
                "Your payment for \"" + txn.getListing().getTitle() + "\" has been initiated.",
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.PAYMENT_INITIATED,
                "Buyer started payment",
                "A payment of " + amount + " has been initiated for your listing \"" + txn.getListing().getTitle() + "\".",
                payload,
                true
        ));
    }

    public void publishPaymentPaid(Payment payment) {
        Transaction txn = payment.getTransaction();
        Map<String, Object> payload = paymentPayload(payment);

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getBuyer().getId(),
                NotificationEventType.PAYMENT_PAID,
                "Payment successful",
                "Your payment for \"" + txn.getListing().getTitle() + "\" was successful and is now held in escrow.",
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.PAYMENT_PAID,
                "Payment received in escrow",
                "Payment for your listing \"" + txn.getListing().getTitle() + "\" was successful and is now held in escrow.",
                payload,
                true
        ));
    }

    public void publishPaymentFailed(Payment payment) {
        Transaction txn = payment.getTransaction();
        Map<String, Object> payload = paymentPayload(payment);

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getBuyer().getId(),
                NotificationEventType.PAYMENT_FAILED,
                "Payment failed",
                "Your payment for \"" + txn.getListing().getTitle() + "\" failed.",
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.PAYMENT_FAILED,
                "Buyer payment failed",
                "The payment attempt for your listing \"" + txn.getListing().getTitle() + "\" failed.",
                payload,
                true
        ));
    }

    public void publishTransactionCompleted(Transaction txn) {
        Map<String, Object> payload = payloadOf(
                "transactionId", txn.getId().toString(),
                "listingId", txn.getListing().getId().toString(),
                "listingTitle", txn.getListing().getTitle(),
                "buyerId", txn.getBuyer().getId().toString(),
                "sellerId", txn.getSeller().getId().toString(),
                "amount", txn.getAmount() == null ? null : txn.getAmount().toPlainString(),
                "status", txn.getStatus().name()
        );

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getBuyer().getId(),
                NotificationEventType.TRANSACTION_COMPLETED,
                "Transaction completed",
                "You completed the transaction for \"" + txn.getListing().getTitle() + "\".",
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.TRANSACTION_COMPLETED,
                "Sale completed",
                "Your transaction for \"" + txn.getListing().getTitle() + "\" has been completed.",
                payload,
                true
        ));
    }

    public void publishPasswordReset(User user, String token) {
        String resetLink = "https://your-app-domain/reset-password?token=" + token;
        eventPublisher.publishEvent(new DomainNotificationEvent(
                user.getId(),
                NotificationEventType.PASSWORD_RESET_REQUESTED,
                "Password Reset Request",
                "You requested a password reset. Use the following link to reset your password: " + resetLink + "\nIf you did not request this, please ignore this email.",
                payloadOf(
                        "userId", user.getId().toString(),
                        "email", user.getEmail(),
                        "resetToken", token
                ),
                true
        ));
    }

    private Map<String, Object> paymentPayload(Payment payment) {
        Transaction txn = payment.getTransaction();
        return payloadOf(
                "paymentId", payment.getId().toString(),
                "transactionId", txn.getId().toString(),
                "listingId", txn.getListing().getId().toString(),
                "listingTitle", txn.getListing().getTitle(),
                "buyerId", txn.getBuyer().getId().toString(),
                "sellerId", txn.getSeller().getId().toString(),
                "amount", payment.getAmount() == null ? null : payment.getAmount().toPlainString(),
                "currency", payment.getCurrency() == null ? null : payment.getCurrency().name(),
                "status", payment.getStatus() == null ? null : payment.getStatus().name(),
                "provider", payment.getProvider() == null ? null : payment.getProvider().name(),
                "paymentReference", payment.getPaymentReference()
        );
    }

    private Map<String, Object> payloadOf(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];
            if (value != null) {
                payload.put(key, value);
            }
        }
        return payload;
    }
}


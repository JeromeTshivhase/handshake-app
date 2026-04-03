package com.trustmarket.handshake.notification.service;

import com.trustmarket.handshake.notification.entity.NotificationEventType;
import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import com.trustmarket.handshake.transaction.entity.Handshake;
import com.trustmarket.handshake.transaction.entity.Payment;
import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    private String safeReplace(String template, String target, String value) {
        return (template == null || template.isEmpty()) ? "" : template.replace(target, value);
    }

    private String safeReplace(String template, String target1, String value1, String target2, String value2) {
        if (template == null || template.isEmpty()) return "";
        return template.replace(target1, value1).replace(target2, value2);
    }

    public void publishHandshakeRequest(com.trustmarket.handshake.transaction.entity.Handshake handshake) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getSeller().getId(),
                NotificationEventType.HANDSHAKE_REQUEST,
                yamlMsg("notification", "handshake", "request"),
                safeReplace(yamlMsg("notification", "handshake", "request_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "agreedPrice", handshake.getAgreedPrice() == null ? null : handshake.getAgreedPrice().toPlainString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
    }

    public void publishHandshakeAccepted(com.trustmarket.handshake.transaction.entity.Handshake handshake) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getBuyer().getId(),
                NotificationEventType.HANDSHAKE_ACCEPTED,
                yamlMsg("notification", "handshake", "accepted"),
                safeReplace(yamlMsg("notification", "handshake", "accepted_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "agreedPrice", handshake.getAgreedPrice() == null ? null : handshake.getAgreedPrice().toPlainString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
    }

    public void publishHandshakeRejected(com.trustmarket.handshake.transaction.entity.Handshake handshake) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getBuyer().getId(),
                NotificationEventType.HANDSHAKE_REJECTED,
                yamlMsg("notification", "handshake", "rejected"),
                safeReplace(yamlMsg("notification", "handshake", "rejected_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "agreedPrice", handshake.getAgreedPrice() == null ? null : handshake.getAgreedPrice().toPlainString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
    }

    public void publishMeetupScheduled(Meetup meetup) {
        var handshake = meetup.getHandshake();
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getBuyer().getId(),
                NotificationEventType.MEETUP_SCHEDULED,
                yamlMsg("notification", "meetup", "scheduled"),
                safeReplace(yamlMsg("notification", "meetup", "scheduled_body"), "{0}", meetup.getAddressLabel()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "meetupId", meetup.getId() != null ? meetup.getId().toString() : null,
                        "scheduledTime", meetup.getScheduledTime() != null ? meetup.getScheduledTime().toString() : null,
                        "latitude", meetup.getLatitude(),
                        "longitude", meetup.getLongitude(),
                        "addressLabel", meetup.getAddressLabel()
                ),
                true
        ));
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getSeller().getId(),
                NotificationEventType.MEETUP_SCHEDULED,
                yamlMsg("notification", "meetup", "scheduled"),
                safeReplace(yamlMsg("notification", "meetup", "scheduled_body"), "{0}", meetup.getAddressLabel()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "meetupId", meetup.getId() != null ? meetup.getId().toString() : null,
                        "scheduledTime", meetup.getScheduledTime() != null ? meetup.getScheduledTime().toString() : null,
                        "latitude", meetup.getLatitude(),
                        "longitude", meetup.getLongitude(),
                        "addressLabel", meetup.getAddressLabel()
                ),
                true
        ));
    }

    public void publishOtpEnabled(Handshake handshake) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getBuyer().getId(),
                NotificationEventType.OTP_ENABLED,
                yamlMsg("notification", "otp", "enabled"),
                safeReplace(yamlMsg("notification", "otp", "enabled_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getSeller().getId(),
                NotificationEventType.OTP_ENABLED,
                yamlMsg("notification", "otp", "enabled"),
                safeReplace(yamlMsg("notification", "otp", "enabled_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
    }

    public void publishOtpVerified(com.trustmarket.handshake.transaction.entity.Handshake handshake) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getBuyer().getId(),
                NotificationEventType.OTP_VERIFIED,
                yamlMsg("notification", "otp", "verified"),
                safeReplace(yamlMsg("notification", "otp", "verified_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getSeller().getId(),
                NotificationEventType.OTP_VERIFIED,
                yamlMsg("notification", "otp", "verified"),
                safeReplace(yamlMsg("notification", "otp", "verified_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
    }

    public void publishHandshakeCancelled(com.trustmarket.handshake.transaction.entity.Handshake handshake) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getBuyer().getId(),
                NotificationEventType.HANDSHAKE_CANCELLED,
                yamlMsg("notification", "handshake", "cancelled"),
                safeReplace(yamlMsg("notification", "handshake", "cancelled_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "agreedPrice", handshake.getAgreedPrice() == null ? null : handshake.getAgreedPrice().toPlainString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
        eventPublisher.publishEvent(new DomainNotificationEvent(
                handshake.getSeller().getId(),
                NotificationEventType.HANDSHAKE_CANCELLED,
                yamlMsg("notification", "handshake", "cancelled"),
                safeReplace(yamlMsg("notification", "handshake", "cancelled_body"), "{0}", handshake.getListing().getTitle()),
                payloadOf(
                        "handshakeId", handshake.getId().toString(),
                        "listingId", handshake.getListing().getId().toString(),
                        "buyerId", handshake.getBuyer().getId().toString(),
                        "sellerId", handshake.getSeller().getId().toString(),
                        "agreedPrice", handshake.getAgreedPrice() == null ? null : handshake.getAgreedPrice().toPlainString(),
                        "status", handshake.getStatus().name()
                ),
                true
        ));
    }

    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.domain:https://your-app-domain}")
    private String appDomain;

    private static final Map<String, Object> yamlMessages;
    static {
        Yaml yaml = new Yaml();
        try (InputStream in = NotificationPublisher.class.getResourceAsStream("/messages.yaml")) {
            yamlMessages = yaml.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load messages.yaml", e);
        }
    }

    private String yamlMsg(String... path) {
        Object node = yamlMessages;
        for (String key : path) {
            if (node instanceof Map) {
                node = ((Map<?, ?>) node).get(key);
            } else {
                // Log missing template path
                System.err.println("[NotificationPublisher] yamlMsg: Path not found or not a map: " + String.join("/", path));
                return "[Missing template: " + String.join("/", path) + "]";
            }
        }
        if (node == null) {
            // Log missing template value
            System.err.println("[NotificationPublisher] yamlMsg: Template missing for path: " + String.join("/", path));
            return "[Missing template: " + String.join("/", path) + "]";
        }
        return node.toString();
    }

    public void publishUserCreated(User user) {
        eventPublisher.publishEvent(new DomainNotificationEvent(
                user.getId(),
                NotificationEventType.USER_CREATED,
                yamlMsg("notification", "user", "created"),
                yamlMsg("notification", "user", "created_body"),
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
                yamlMsg("notification", "payment", "initiated"),
                safeReplace(yamlMsg("notification", "payment", "initiated_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.PAYMENT_INITIATED,
                yamlMsg("notification", "payment", "initiated_seller"),
                safeReplace(yamlMsg("notification", "payment", "initiated_seller_body"), "{0}", amount == null ? "" : amount, "{1}", txn.getListing().getTitle()),
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
                yamlMsg("notification", "payment", "paid"),
                safeReplace(yamlMsg("notification", "payment", "paid_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.PAYMENT_PAID,
                yamlMsg("notification", "payment", "paid_seller"),
                safeReplace(yamlMsg("notification", "payment", "paid_seller_body"), "{0}", txn.getListing().getTitle()),
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
                yamlMsg("notification", "payment", "failed"),
                safeReplace(yamlMsg("notification", "payment", "failed_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.PAYMENT_FAILED,
                yamlMsg("notification", "payment", "failed_seller"),
                safeReplace(yamlMsg("notification", "payment", "failed_seller_body"), "{0}", txn.getListing().getTitle()),
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
                yamlMsg("notification", "transaction", "completed"),
                safeReplace(yamlMsg("notification", "transaction", "completed_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.TRANSACTION_COMPLETED,
                yamlMsg("notification", "transaction", "completed_seller"),
                safeReplace(yamlMsg("notification", "transaction", "completed_seller_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));
    }

    public void publishEscrowReleased(Transaction txn) {
        Map<String, Object> payload = payloadOf(
                "transactionId", txn.getId().toString(),
                "listingId", txn.getListing().getId().toString(),
                "listingTitle", txn.getListing().getTitle(),
                "buyerId", txn.getBuyer().getId().toString(),
                "sellerId", txn.getSeller().getId().toString(),
                "amount", txn.getAmount() == null ? null : txn.getAmount().toPlainString(),
                "status", txn.getStatus().name(),
                "escrowReleaseAt", txn.getEscrowReleaseAt() == null ? null : txn.getEscrowReleaseAt().toString()
        );

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getBuyer().getId(),
                NotificationEventType.ESCROW_RELEASED,
                yamlMsg("notification", "escrow", "released"),
                safeReplace(yamlMsg("notification", "escrow", "released_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));

        eventPublisher.publishEvent(new DomainNotificationEvent(
                txn.getSeller().getId(),
                NotificationEventType.ESCROW_RELEASED,
                yamlMsg("notification", "escrow", "released_seller"),
                safeReplace(yamlMsg("notification", "escrow", "released_seller_body"), "{0}", txn.getListing().getTitle()),
                payload,
                true
        ));
    }

    public void publishPasswordReset(User user, String token) {
        String resetLink = appDomain + "/reset-password?token=" + token;
        eventPublisher.publishEvent(new DomainNotificationEvent(
                user.getId(),
                NotificationEventType.PASSWORD_RESET_REQUESTED,
                yamlMsg("notification", "password", "reset"),
                safeReplace(yamlMsg("notification", "password", "reset_body"), "{0}", resetLink),
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

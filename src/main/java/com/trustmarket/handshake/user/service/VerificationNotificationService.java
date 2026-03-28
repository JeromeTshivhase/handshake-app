package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.verification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.verification.email.from:${app.messaging.email.from:no-reply@handshake.local}}")
    private String fromAddress;

    @Value("${app.verification.sms.enabled:false}")
    private boolean smsEnabled;

    public void sendEmailOtp(User user, String email, String otp, LocalDateTime expiresAt) {
        if (!emailEnabled) {
            log.info("Email verification delivery is disabled for user {}", user.getId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email verification OTP generated for user {} but no JavaMailSender is configured", user.getId());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            mail.setFrom(fromAddress);
        }
        mail.setTo(email);
        mail.setSubject("Your Handshake email verification code");
        mail.setText(buildEmailBody(user, otp, expiresAt));

        try {
            mailSender.send(mail);
        } catch (Exception e) {
            log.warn("Failed to send email verification OTP to {}: {}", email, e.getMessage());
        }
    }

    public void sendPhoneOtp(User user, String phone, String otp, LocalDateTime expiresAt) {
        if (smsEnabled) {
            log.info("SMS verification OTP dispatch requested for user {} to {} (provider not configured in this project yet)", user.getId(), phone);
            return;
        }

        log.info("Phone verification OTP generated for user {}. SMS delivery is disabled; configure app.verification.sms.enabled and an SMS provider to send codes. Target={}, expiresAt={}",
                user.getId(), maskPhone(phone), expiresAt);
        log.debug("Phone verification OTP for user {} is {}", user.getId(), otp);
    }

    private String buildEmailBody(User user, String otp, LocalDateTime expiresAt) {
        String firstName = user.getFirstName() == null || user.getFirstName().isBlank() ? "there" : user.getFirstName().trim();
        long minutes = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), expiresAt));

        return "Hi " + firstName + ",\n\n"
                + "Your Handshake email verification code is: " + otp + "\n\n"
                + "This code expires in about " + minutes + " minute(s).\n"
                + "If you did not request this code, you can ignore this email.\n\n"
                + "- Handshake";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        return "*".repeat(Math.max(0, phone.length() - 4)) + phone.substring(phone.length() - 4);
    }
}


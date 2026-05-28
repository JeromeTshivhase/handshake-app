package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {"verification.sms.enabled=true"})
public class VerificationNotificationServiceTest {

    @Autowired
    private VerificationNotificationService notificationService;

    @MockBean
    private SmsSender smsSender;

    @Test
    public void sendPhoneOtp_usesSmsSender_whenEnabled() {
        User user = User.builder().id(UUID.randomUUID()).firstName("Test").build();
        String phone = "+254700000000";
        String otp = "123456";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        notificationService.sendPhoneOtp(user, phone, otp, expiresAt);

        verify(smsSender).sendSms(eq(user), eq(phone), contains(otp));
    }
}



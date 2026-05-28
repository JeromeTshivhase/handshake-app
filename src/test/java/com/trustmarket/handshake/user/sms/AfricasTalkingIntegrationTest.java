package com.trustmarket.handshake.user.sms;

import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.sms.AfricasTalkingSmsSender;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Integration test that will attempt a real send only if environment variables are present.
 * Set AFRICASTALKING_API_KEY and AFRICASTALKING_USERNAME and AFRICASTALKING_TO to run this test.
 */
@SpringBootTest
public class AfricasTalkingIntegrationTest {

    @Autowired(required = false)
    private AfricasTalkingSmsSender sender;

    @Test
    public void realSend_ifConfigured() {
        String apiKey = System.getenv("AFRICASTALKING_API_KEY");
        String username = System.getenv("AFRICASTALKING_USERNAME");
        String to = System.getenv("AFRICASTALKING_TO");

        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "AFRICASTALKING_API_KEY not set");
        Assumptions.assumeTrue(username != null && !username.isBlank(), "AFRICASTALKING_USERNAME not set");
        Assumptions.assumeTrue(to != null && !to.isBlank(), "AFRICASTALKING_TO not set");
        Assumptions.assumeTrue(sender != null, "AfricasTalkingSmsSender bean not present (enable verification.sms.africastalking.enabled=true)");

        User user = User.builder().id(UUID.randomUUID()).firstName("IT").build();
        String code = "000000";
        sender.sendSms(user, to, "Integration test OTP: " + code + " - ignore");
    }
}


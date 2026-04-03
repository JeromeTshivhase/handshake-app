package com.trustmarket.handshake.messaging.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class MessageSafetyServiceTest {
    private final MessageSafetyService service = new MessageSafetyService();
    @Test
    void assessFlagsAndMasksSuspiciousUrl() {
        var result = service.assess("Check https://payfa5t.xyz/login now");
        assertTrue(result.flags().contains("PHISHING_URL"));
        assertTrue(result.highRisk());
        assertTrue(result.sanitizedContent().contains("[BLOCKED_URL]"));
    }
    @Test
    void assessFlagsSocialHandleAndPhone() {
        var result = service.assess("message me @seller_handle or +27 82 123 4567");
        assertTrue(result.flags().contains("SOCIAL_MEDIA_HANDLE"));
        assertTrue(result.flags().contains("PHONE_NUMBER"));
        assertFalse(result.highRisk());
    }
    @Test
    void assessSafeMessage() {
        var result = service.assess("Hi, is this item still available?");
        assertTrue(result.flags().isEmpty());
        assertFalse(result.highRisk());
        assertTrue(result.sanitizedContent().contains("available"));
    }
}

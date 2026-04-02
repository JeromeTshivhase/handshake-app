package com.trustmarket.handshake.safe_meetup.service;

import com.trustmarket.handshake.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OtpRevealCryptoServiceTest {

    private OtpRevealCryptoService service;

    @BeforeEach
    void setUp() {
        service = new OtpRevealCryptoService();
        // 32-byte fixed test key.
        String keyB64 = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        ReflectionTestUtils.setField(service, "revealKeyB64", keyB64);
        ReflectionTestUtils.invokeMethod(service, "init");
    }

    @Test
    void encryptDecryptRoundTrip() {
        UUID handshakeId = UUID.randomUUID();
        String encrypted = service.encrypt(handshakeId, "123456");

        String decrypted = service.decrypt(handshakeId, encrypted);

        assertEquals("123456", decrypted);
    }

    @Test
    void decryptFailsWhenHandshakeIdDiffers() {
        UUID originalHandshake = UUID.randomUUID();
        String encrypted = service.encrypt(originalHandshake, "123456");

        assertThrows(BadRequestException.class, () -> service.decrypt(UUID.randomUUID(), encrypted));
    }
}


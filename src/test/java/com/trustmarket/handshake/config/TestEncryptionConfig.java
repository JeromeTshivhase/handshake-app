package com.trustmarket.handshake.config;

import org.springframework.boot.test.context.TestConfiguration;

import jakarta.annotation.PostConstruct;

/**
 * Sets the HANDSHAKE_MASTER_KEY system property for tests so that
 * {@link com.trustmarket.handshake.shared.security.EncryptionConverter} can initialise.
 *
 * The key is a fixed 32-byte value (base64-encoded) that is ONLY used in the test environment.
 * Never use this key value in production.
 */
@TestConfiguration
public class TestEncryptionConfig {

    // 32 zero-bytes, base64-encoded — safe for tests only
    private static final String TEST_KEY_B64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @PostConstruct
    public void setTestMasterKey() {
        if (System.getProperty("handshake.master.key") == null
                && System.getenv("HANDSHAKE_MASTER_KEY") == null) {
            System.setProperty("handshake.master.key", TEST_KEY_B64);
        }
    }
}

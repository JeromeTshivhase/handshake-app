package com.trustmarket.handshake.shared.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter that encrypts/decrypts String fields using AES-256-GCM.
 *
 * Usage:
 *  @Convert(converter = EncryptionConverter.class)
 *  private String accountNumber;
 *
 * Configuration:
 *  - Provide a 32-byte Base64-encoded master key via env var HANDSHAKE_MASTER_KEY
 *    or system property handshake.master.key. Example (bash):
 *      export HANDSHAKE_MASTER_KEY=$(openssl rand -base64 32)
 *
 * Notes:
 *  - This is an application-level converter for encrypting values at rest.
 *  - For production use, prefer envelope encryption with a KMS (wrap/unwrap DEKs) and
 *    rotate keys via the KMS. This converter is intentionally minimal so it is
 *    straightforward to replace the key retrieval with a KMS call later.
 */
@Converter(autoApply = false)
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH_BYTES = 12; // recommended for GCM
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final SecretKey MASTER_KEY = loadMasterKey();

    private static SecretKey loadMasterKey() {
        try {
            String b64 = System.getenv("HANDSHAKE_MASTER_KEY");
            if (b64 == null || b64.isBlank()) {
                b64 = System.getProperty("handshake.master.key");
            }
            if (b64 == null || b64.isBlank()) {
                throw new IllegalStateException(
                        "HANDSHAKE_MASTER_KEY environment variable is not set. " +
                        "Generate a key with: openssl rand -base64 32");
            }
            byte[] keyBytes = Base64.getDecoder().decode(b64);
            if (keyBytes.length != 32) {
                throw new IllegalStateException("HANDSHAKE_MASTER_KEY must be 32 bytes when base64-decoded (AES-256)");
            }
            return new SecretKeySpec(keyBytes, AES);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize master encryption key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, MASTER_KEY, spec);
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(dbData);
            if (all.length < IV_LENGTH_BYTES) {
                throw new IllegalStateException("Invalid ciphertext stored in DB");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH_BYTES);
            int ctLen = all.length - IV_LENGTH_BYTES;
            byte[] ct = new byte[ctLen];
            System.arraycopy(all, IV_LENGTH_BYTES, ct, 0, ctLen);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, MASTER_KEY, spec);
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}



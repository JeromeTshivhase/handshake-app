package com.trustmarket.handshake.safe_meetup.service;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
@Service
public class OtpRevealCryptoService {
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.otp.reveal-key-b64:Qzc0b0d6Sm81R3V0QjJqYzBhdDZOYWkzQ0R2VndwM3Q=}")
    private String revealKeyB64;
    private SecretKey secretKey;
    @PostConstruct
    void init() {
        byte[] decoded = Base64.getDecoder().decode(revealKeyB64);
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new IllegalStateException("app.otp.reveal-key-b64 must decode to 16, 24, or 32 bytes");
        }
        this.secretKey = new SecretKeySpec(decoded, "AES");
    }
    public String encrypt(UUID handshakeId, String otp) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(handshakeId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(otp.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to secure OTP for reveal");
        }
    }
    public String decrypt(UUID handshakeId, String encryptedOtp) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedOtp);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted OTP payload");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(handshakeId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BadRequestException("OTP not available. Re-enable OTP at meetup location.");
        }
    }
}

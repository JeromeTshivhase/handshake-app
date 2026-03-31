package com.trustmarket.handshake.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class TotpService {
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().withoutPadding().encodeToString(buffer);
    }

    public boolean verifyCode(String base64Secret, String code) {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int i = -1; i <= 1; i++) {
            String candidate = generateCode(base64Secret, timeIndex + i);
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String generateCode(String base64Secret, long timeIndex) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (timeIndex & 0xFF);
                timeIndex >>= 8;
            }
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }
}

package com.trustmarket.handshake.auth.service;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class TotpService {
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";

    public SetupData generateSetupData(String issuer, String email) {
        String secret = generateSecret();
        String label = issuer + ":" + email;
        String provisioningUri = "otpauth://totp/" + urlEncode(label)
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(issuer);
        // Google Chart QR endpoint is deprecated; QuickChart provides a compatible replacement.
        String qrCodeUrl = "https://quickchart.io/qr?size=256&text=" + urlEncode(provisioningUri);
        return SetupData.builder()
                .secretKey(secret)
                .provisioningUri(provisioningUri)
                .qrCodeUrl(qrCodeUrl)
                .build();
    }

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }

    public boolean verifyCode(String base32Secret, String code) {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int i = -1; i <= 1; i++) {
            String candidate = generateCode(base32Secret, timeIndex + i);
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String generateCode(String base32Secret, long timeIndex) {
        try {
            byte[] key = new Base32().decode(base32Secret);
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

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Getter
    @Builder
    public static class SetupData {
        private final String secretKey;
        private final String provisioningUri;
        private final String qrCodeUrl;
    }
}

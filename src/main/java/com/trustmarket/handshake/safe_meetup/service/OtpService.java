package com.trustmarket.handshake.safe_meetup.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {

    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public OtpService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generate6Digits() {
        int n = 100000 + random.nextInt(900000);
        return String.valueOf(n);
    }

    public String hash(String otp) {
        return passwordEncoder.encode(otp);
    }

    public boolean matches(String otp, String hash) {
        return passwordEncoder.matches(otp, hash);
    }
}
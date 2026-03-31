package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TwoFaService {
    private final UserRepository userRepository;
    private final TotpService totpService;

    public void enable(UUID userId, String method) {
        User user = getUser(userId);
        if (user.isTwoFaEnabled()) {
            throw new BadRequestException("2FA is already enabled");
        }
        if (!"app".equalsIgnoreCase(method)) {
            throw new BadRequestException("Only app-based 2FA is supported");
        }
        String secret = totpService.generateSecret();
        user.setTwoFaSecret(secret);
        user.setTwoFaEnabled(true);
        userRepository.save(user);
        // Optionally: send QR or secret to user
    }

    public void disable(UUID userId) {
        User user = getUser(userId);
        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
    }

    public void verify(UUID userId, String code) {
        User user = getUser(userId);
        if (!user.isTwoFaEnabled() || user.getTwoFaSecret() == null) {
            throw new BadRequestException("2FA is not enabled");
        }
        boolean valid = totpService.verifyCode(user.getTwoFaSecret(), code);
        if (!valid) {
            throw new BadRequestException("Invalid 2FA code");
        }
    }

    public boolean isEnabled(UUID userId) {
        User user = getUser(userId);
        return user.isTwoFaEnabled();
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}


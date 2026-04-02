package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.auth.dto.TwoFaEnableRequest;
import com.trustmarket.handshake.auth.dto.TwoFaSetupRequest;
import com.trustmarket.handshake.auth.dto.TwoFaSetupResponse;
import com.trustmarket.handshake.auth.dto.TwoFaStatusResponse;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TwoFaService {
    private static final int SETUP_TIMEOUT_MINUTES = 10;
    private static final int RECOVERY_CODE_COUNT = 8;

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.2fa.issuer:Handshake}")
    private String issuer;

    @Transactional
    public TwoFaSetupResponse initiateSetup(UUID userId, TwoFaSetupRequest request) {
        User user = getUser(userId);
        if (user.isTwoFaEnabled()) {
            throw new BadRequestException("2FA is already enabled for this account");
        }

        String method = request.getMethod() == null ? "app" : request.getMethod().trim();
        if (!"app".equalsIgnoreCase(method)) {
            throw new BadRequestException("Only app-based 2FA is currently supported");
        }

        TotpService.SetupData setupData = totpService.generateSetupData(issuer, user.getEmail());
        user.setTwoFaSetupSecret(setupData.getSecretKey());
        user.setTwoFaSetupCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        return TwoFaSetupResponse.builder()
                .secretKey(setupData.getSecretKey())
                .provisioningUri(setupData.getProvisioningUri())
                .qrCodeUrl(setupData.getQrCodeUrl())
                .build();
    }

    @Transactional
    public List<String> enable(UUID userId, TwoFaEnableRequest request) {
        User user = getUser(userId);
        if (user.isTwoFaEnabled()) {
            throw new BadRequestException("2FA is already enabled");
        }
        if (user.getTwoFaSetupSecret() == null || user.getTwoFaSetupCreatedAt() == null) {
            throw new BadRequestException("No pending 2FA setup. Please setup 2FA first.");
        }
        if (user.getTwoFaSetupCreatedAt().plusMinutes(SETUP_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            clearSetup(user);
            userRepository.save(user);
            throw new BadRequestException("2FA setup has expired. Please setup 2FA again.");
        }

        String code = normalizeCode(request.getVerificationCode());
        if (!totpService.verifyCode(user.getTwoFaSetupSecret(), code)) {
            throw new BadRequestException("Invalid verification code");
        }

        List<String> recoveryCodes = generateRecoveryCodes();

        user.setTwoFaEnabled(true);
        user.setTwoFaSecret(user.getTwoFaSetupSecret());
        user.setTwoFaMethod("app");
        user.setTwoFaEnabledAt(LocalDateTime.now());
        user.setRecoveryCodes(hashRecoveryCodes(recoveryCodes));
        clearSetup(user);
        userRepository.save(user);

        return recoveryCodes;
    }

    @Transactional
    public void disable(UUID userId, String password) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Invalid password");
        }

        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        user.setTwoFaMethod(null);
        user.setTwoFaEnabledAt(null);
        user.setRecoveryCodes(null);
        clearSetup(user);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TwoFaStatusResponse getStatus(UUID userId) {
        User user = getUser(userId);
        return TwoFaStatusResponse.builder()
                .enabled(user.isTwoFaEnabled())
                .method(user.isTwoFaEnabled() ? user.getTwoFaMethod() : null)
                .maskedEmail(maskEmail(user.getEmail()))
                .build();
    }

    @Transactional(readOnly = true)
    public boolean verifyDuringLogin(UUID userId, String code) {
        User user = getUser(userId);
        if (!user.isTwoFaEnabled()) {
            return true;
        }
        if (user.getTwoFaSecret() == null) {
            return false;
        }
        return totpService.verifyCode(user.getTwoFaSecret(), normalizeCode(code));
    }

    @Transactional
    public boolean verifyRecoveryCode(UUID userId, String recoveryCode) {
        User user = getUser(userId);
        if (!user.isTwoFaEnabled()) {
            return true;
        }

        List<String> storedCodes = parseRecoveryCodes(user.getRecoveryCodes());
        for (int i = 0; i < storedCodes.size(); i++) {
            if (passwordEncoder.matches(recoveryCode, storedCodes.get(i))) {
                storedCodes.remove(i);
                user.setRecoveryCodes(String.join(",", storedCodes));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId, String verificationCode) {
        User user = getUser(userId);
        if (!user.isTwoFaEnabled()) {
            throw new BadRequestException("2FA is not enabled");
        }

        if (!totpService.verifyCode(user.getTwoFaSecret(), normalizeCode(verificationCode))) {
            throw new BadRequestException("Invalid verification code");
        }

        List<String> newCodes = generateRecoveryCodes();
        user.setRecoveryCodes(hashRecoveryCodes(newCodes));
        userRepository.save(user);
        return newCodes;
    }

    public void verify(UUID userId, String code) {
        User user = getUser(userId);
        if (!user.isTwoFaEnabled() || user.getTwoFaSecret() == null) {
            throw new BadRequestException("2FA is not enabled");
        }
        if (!totpService.verifyCode(user.getTwoFaSecret(), normalizeCode(code))) {
            throw new BadRequestException("Invalid 2FA code");
        }
    }

    public boolean isEnabled(UUID userId) {
        return getUser(userId).isTwoFaEnabled();
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new BadRequestException("Verification code is required");
        }
        String normalized = code.trim();
        if (!normalized.matches("\\d{6}")) {
            throw new BadRequestException("Verification code must be a 6-digit number");
        }
        return normalized;
    }

    private void clearSetup(User user) {
        user.setTwoFaSetupSecret(null);
        user.setTwoFaSetupCreatedAt(null);
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            codes.add(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return codes;
    }

    private String hashRecoveryCodes(List<String> codes) {
        return codes.stream().map(passwordEncoder::encode).collect(Collectors.joining(","));
    }

    private List<String> parseRecoveryCodes(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(serialized.split(",")));
    }

    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 2) {
            return email;
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}

package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.security.JwtService;
import com.trustmarket.handshake.user.entity.RefreshToken;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.UserRole;
import com.trustmarket.handshake.user.repository.RefreshTokenRepository;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.service.UserProfileService;
import com.trustmarket.handshake.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import com.trustmarket.handshake.auth.entity.PasswordResetToken;
import com.trustmarket.handshake.auth.repository.PasswordResetTokenRepository;
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final NotificationPublisher notificationPublisher;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    @Value("${jwt.access-expiration-seconds}")
    private long accessExpSeconds;

    @Value("${jwt.refresh-expiration-seconds}")
    private long refreshExpSeconds;

    public User register(String email,
                         String password,
                         String passwordConfirm,
                         String firstName,
                         String lastName,
                         String phone,
                         String city,
                         String province) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already in use");
        }
        if (!password.equals(passwordConfirm)) {
            throw new BadRequestException("Password and passwordConfirm do not match");
        }

        User user = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .phone(phone == null ? null : phone.trim())
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);
        userProfileService.updateProfile(user.getId(), null, null, city, province);
        notificationPublisher.publishUserCreated(user);
        return user;
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        userService.assertNotLocked(user);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            userService.recordLoginFailure(user);
            throw new BadRequestException("Invalid email or password");
        }

        userService.recordLoginSuccess(user);
        return user;
    }

    public String newAccessToken(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    public String issueAndStoreRefreshToken(User user) {
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(refresh)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpSeconds))
                .revoked(false)
                .build();

        refreshTokenRepository.save(rt);
        return refresh;
    }

    public String rotateRefreshToken(String oldRefreshToken) {
        if (!jwtService.isValid(oldRefreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        Claims claims = jwtService.parse(oldRefreshToken);
        if (!"REFRESH".equals(claims.get("type"))) {
            throw new BadRequestException("Token is not a refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token not recognized"));

        if (stored.isRevoked()) {
            throw new BadRequestException("Refresh token revoked");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token expired");
        }

        // revoke old
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        // issue new
        User user = stored.getUser();
        return issueAndStoreRefreshToken(user);
    }

    public long accessExpiresInSeconds() { return accessExpSeconds; }
    public long refreshExpiresInSeconds() { return refreshExpSeconds; }
    
    // Forgot password: generate and email a reset token
    public void forgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("No user found with that email address"));
        // Invalidate previous tokens
        passwordResetTokenRepository.deleteByUserId(user.getId());
        // Generate token
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(java.time.LocalDateTime.now().plusHours(2))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        // Send email
        notificationPublisher.publishPasswordReset(user, token);
    }

    // Reset password using token
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired password reset token");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // Change password for logged-in user
    public void changePassword(UUID userId, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Enable 2FA for user
    public void enable2fa(UUID userId, String method) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.isTwoFaEnabled()) {
            throw new BadRequestException("2FA is already enabled");
        }
        if (!"app".equalsIgnoreCase(method)) {
            throw new BadRequestException("Only app-based 2FA is supported");
        }
        // Generate TOTP secret
        String secret = com.trustmarket.handshake.auth.util.TotpUtil.generateSecret();
        user.setTwoFaSecret(secret);
        user.setTwoFaEnabled(true);
        userRepository.save(user);
        // Optionally: send QR code or secret to user
    }

    // Disable 2FA for user
    public void disable2fa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
    }

    // Verify 2FA code
    public void verify2fa(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (!user.isTwoFaEnabled() || user.getTwoFaSecret() == null) {
            throw new BadRequestException("2FA is not enabled");
        }
        boolean valid = com.trustmarket.handshake.auth.util.TotpUtil.verifyCode(user.getTwoFaSecret(), code);
        if (!valid) {
            throw new BadRequestException("Invalid 2FA code");
        }
    }

    // Get 2FA status
    public boolean get2faStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return user.isTwoFaEnabled();
    }
}


package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.shared.notification.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService,
            UserProfileService userProfileService,
            @Qualifier("userNotificationService") NotificationService notificationService,
            TwoFaService twoFaService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            com.trustmarket.handshake.user.service.UserVerificationService userVerificationService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.notificationService = notificationService;
        this.twoFaService = twoFaService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userVerificationService = userVerificationService;
    }
    private final TwoFaService twoFaService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.trustmarket.handshake.user.service.UserVerificationService userVerificationService;
    @Value("${jwt.access-expiration-seconds}")
    private long accessExpSeconds;

    @Value("${jwt.refresh-expiration-seconds}")
    private long refreshExpSeconds;

    public com.trustmarket.handshake.auth.dto.AuthResponse register(com.trustmarket.handshake.auth.dto.RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        if (!req.getPassword().equals(req.getPasswordConfirm())) {
            throw new BadRequestException("Password and passwordConfirm do not match");
        }
        User user = User.builder()
                .email(req.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .phone(req.getPhone() == null ? null : req.getPhone().trim())
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user);
        userProfileService.updateProfile(user.getId(), null, null, req.getCity(), req.getProvince());
        notificationService.sendUserCreated(user);
        try {
            userVerificationService.requestEmailOtp(user.getId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Failed to send email OTP: {}", e.getMessage());
        }
        String access = newAccessToken(user);
        String refresh = issueAndStoreRefreshToken(user);
        return buildAuthResponse(user, access, refresh);
    }

    public com.trustmarket.handshake.auth.dto.AuthResponse login(com.trustmarket.handshake.auth.dto.LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        userService.assertNotLocked(user);
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            userService.recordLoginFailure(user);
            throw new BadRequestException("Invalid email or password");
        }
        userService.recordLoginSuccess(user);
        String access = newAccessToken(user);
        String refresh = issueAndStoreRefreshToken(user);
        return buildAuthResponse(user, access, refresh);
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

    public com.trustmarket.handshake.auth.dto.AuthResponse refresh(com.trustmarket.handshake.auth.dto.RefreshRequest req) {
        String oldRefreshToken = req.getRefreshToken();
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
        String newRefresh = issueAndStoreRefreshToken(user);
        String newAccess = newAccessToken(user);
        return buildAuthResponse(user, newAccess, newRefresh);
    }

    public long accessExpiresInSeconds() { return accessExpSeconds; }
    public long refreshExpiresInSeconds() { return refreshExpSeconds; }
    
    // Forgot password: generate and email a reset token
    public void forgotPassword(com.trustmarket.handshake.auth.dto.ForgotPasswordRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new BadRequestException("No user found with that email address"));
        passwordResetTokenRepository.deleteByUserId(user.getId());
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(java.time.LocalDateTime.now().plusHours(2))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        notificationService.sendPasswordReset(user, token);
    }

    // Reset password using token
    public void resetPassword(com.trustmarket.handshake.auth.dto.ResetPasswordRequest req) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired password reset token");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // Change password for logged-in user
    public void changePassword(UUID userId, com.trustmarket.handshake.auth.dto.ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // 2FA orchestration
    public void enable2fa(UUID userId, com.trustmarket.handshake.auth.dto.TwoFaEnableRequest req) { twoFaService.enable(userId, req.getMethod()); }
    public void disable2fa(UUID userId) { twoFaService.disable(userId); }
    public void verify2fa(UUID userId, com.trustmarket.handshake.auth.dto.TwoFaVerifyRequest req) { twoFaService.verify(userId, req.getCode()); }
    public boolean get2faStatus(UUID userId) { return twoFaService.isEnabled(userId); }
    private com.trustmarket.handshake.auth.dto.AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return com.trustmarket.handshake.auth.dto.AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresInSeconds(accessExpSeconds)
                .refreshExpiresInSeconds(refreshExpSeconds)
                .build();
    }
}

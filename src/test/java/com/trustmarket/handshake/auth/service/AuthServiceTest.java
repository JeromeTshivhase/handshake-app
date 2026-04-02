package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.auth.dto.LoginRequest;
import com.trustmarket.handshake.auth.dto.RegisterRequest;
import com.trustmarket.handshake.auth.repository.PasswordResetTokenRepository;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.notification.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import com.trustmarket.handshake.shared.security.JwtService;
import com.trustmarket.handshake.user.entity.UserRole;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.RefreshTokenRepository;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.service.UserProfileService;
import com.trustmarket.handshake.user.service.UserService;
import com.trustmarket.handshake.user.service.UserVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserService userService;
    @Mock
    private UserVerificationService userVerificationService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    @Qualifier("userNotificationService")
    private NotificationService notificationService;
    @Mock
    private TwoFaService twoFaService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                userService,
                userProfileService,
                notificationService,
                twoFaService,
                passwordResetTokenRepository,
                userVerificationService
        );
        ReflectionTestUtils.setField(authService, "accessExpSeconds", 3600L);
        ReflectionTestUtils.setField(authService, "refreshExpSeconds", 86400L);
    }

    @Test
    void registerPublishesUserCreatedNotification() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        RegisterRequest req = RegisterRequest.builder()
                .email("user@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .firstName("Test")
                .lastName("User")
                .phone("+27123456789")
                .city("Cape Town")
                .province("Western Cape")
                .build();

        authService.register(req);

        verify(userRepository).save(any(User.class));
        verify(notificationService).sendUserCreated(any(User.class));
    }

    @Test
    void loginRequiresTwoFaCodeWhenTwoFaEnabled() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("encoded")
                .role(UserRole.USER)
                .twoFaEnabled(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        LoginRequest req = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        assertThrows(BadRequestException.class, () -> authService.login(req));

        verify(userService, never()).recordLoginSuccess(any(User.class));
    }

    @Test
    void loginAcceptsRecoveryCodeWhenTwoFaEnabled() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("encoded")
                .role(UserRole.USER)
                .twoFaEnabled(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(twoFaService.verifyRecoveryCode(eq(user.getId()), eq("ABCD1234"))).thenReturn(true);
        when(jwtService.generateAccessToken(any(UUID.class), eq("user@example.com"), eq("USER"))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(UUID.class), eq("user@example.com"), eq("USER"))).thenReturn("refresh");

        LoginRequest req = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .recoveryCode("ABCD1234")
                .build();

        authService.login(req);

        verify(userService).recordLoginSuccess(user);
        verify(twoFaService).verifyRecoveryCode(user.getId(), "ABCD1234");
    }
}

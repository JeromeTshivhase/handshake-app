package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.auth.dto.RegisterRequest;
import com.trustmarket.handshake.auth.repository.PasswordResetTokenRepository;
import com.trustmarket.handshake.notification.service.NotificationPublisher;
import com.trustmarket.handshake.shared.notification.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import com.trustmarket.handshake.shared.security.JwtService;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.RefreshTokenRepository;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.service.UserService;
import com.trustmarket.handshake.user.service.UserVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private NotificationPublisher notificationPublisher;
    @Mock
    private UserVerificationService userVerificationService;
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
                notificationService,
                twoFaService,
                passwordResetTokenRepository,
                userVerificationService
        );
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
}

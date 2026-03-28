package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.safe_meetup.service.OtpService;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.dto.UserVerificationStatusResponse;
import com.trustmarket.handshake.user.dto.VerificationDispatchResponse;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.entity.VerificationOtpChallenge;
import com.trustmarket.handshake.user.entity.VerificationPurpose;
import com.trustmarket.handshake.user.repository.UserRepository;
import com.trustmarket.handshake.user.repository.VerificationOtpChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationOtpChallengeRepository challengeRepository;
    @Mock
    private VerificationNotificationService notificationService;
    @Mock
    private OtpService otpService;

    private UserVerificationService userVerificationService;

    @BeforeEach
    void setUp() {
        userVerificationService = new UserVerificationService(
                userService,
                userRepository,
                challengeRepository,
                notificationService,
                otpService
        );
        ReflectionTestUtils.setField(userVerificationService, "otpExpiresMinutes", 10L);
        ReflectionTestUtils.setField(userVerificationService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(userVerificationService, "lockMinutes", 15L);
    }

    @Test
    void requestEmailOtpCreatesChallengeAndDispatchesEmail() {
        User user = baseUser();
        when(userService.getById(user.getId())).thenReturn(user);
        when(otpService.generate6Digits()).thenReturn("123456");
        when(otpService.hash("123456")).thenReturn("hashed-otp");
        when(challengeRepository.findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), VerificationPurpose.EMAIL))
                .thenReturn(Optional.empty());

        VerificationDispatchResponse response = userVerificationService.requestEmailOtp(user.getId());

        assertEquals("EMAIL", response.getPurpose());
        assertTrue(response.getDestination().endsWith("@example.com"));
        assertNotNull(response.getExpiresAt());
        verify(notificationService).sendEmailOtp(eq(user), eq(user.getEmail()), eq("123456"), any(LocalDateTime.class));
        verify(challengeRepository).save(any(VerificationOtpChallenge.class));
    }

    @Test
    void verifyEmailOtpMarksUserAsVerified() {
        User user = baseUser();
        VerificationOtpChallenge challenge = VerificationOtpChallenge.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .purpose(VerificationPurpose.EMAIL)
                .targetValue(user.getEmail())
                .otpCodeHash("hashed-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .lastSentAt(LocalDateTime.now())
                .build();

        when(userService.getById(user.getId())).thenReturn(user);
        when(challengeRepository.findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), VerificationPurpose.EMAIL))
                .thenReturn(Optional.of(challenge));
        when(otpService.matches("123456", "hashed-otp")).thenReturn(true);

        UserVerificationStatusResponse response = userVerificationService.verifyEmailOtp(user.getId(), "123456");

        assertTrue(response.isEmailVerified());
        assertNotNull(response.getEmailVerifiedAt());
        assertTrue(user.isEmailVerified());
        assertNotNull(challenge.getConsumedAt());
        verify(userRepository).save(user);
        verify(challengeRepository).save(challenge);
    }

    @Test
    void verifyEmailOtpLocksChallengeAfterTooManyFailures() {
        User user = baseUser();
        VerificationOtpChallenge challenge = VerificationOtpChallenge.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .purpose(VerificationPurpose.EMAIL)
                .targetValue(user.getEmail())
                .otpCodeHash("hashed-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .failedAttempts(4)
                .lastSentAt(LocalDateTime.now())
                .build();

        when(userService.getById(user.getId())).thenReturn(user);
        when(challengeRepository.findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), VerificationPurpose.EMAIL))
                .thenReturn(Optional.of(challenge));
        when(otpService.matches("000000", "hashed-otp")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userVerificationService.verifyEmailOtp(user.getId(), "000000"));

        assertEquals("Invalid OTP", ex.getMessage());
        assertEquals(5, challenge.getFailedAttempts());
        assertNotNull(challenge.getLockedUntil());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void requestPhoneOtpRequiresPhoneNumber() {
        User user = baseUser();
        user.setPhone(null);
        when(userService.getById(user.getId())).thenReturn(user);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userVerificationService.requestPhoneOtp(user.getId()));

        assertEquals("Phone number is required", ex.getMessage());
    }

    private User baseUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("secret")
                .firstName("Test")
                .lastName("User")
                .phone("+27123456789")
                .emailVerified(false)
                .phoneVerified(false)
                .build();
    }
}


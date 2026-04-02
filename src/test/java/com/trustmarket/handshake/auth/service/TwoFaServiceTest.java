package com.trustmarket.handshake.auth.service;

import com.trustmarket.handshake.auth.dto.TwoFaEnableRequest;
import com.trustmarket.handshake.auth.dto.TwoFaSetupRequest;
import com.trustmarket.handshake.auth.dto.TwoFaSetupResponse;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TotpService totpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TwoFaService twoFaService;

    @BeforeEach
    void setUp() {
        twoFaService = new TwoFaService(userRepository, totpService, passwordEncoder);
        ReflectionTestUtils.setField(twoFaService, "issuer", "Handshake");
    }

    @Test
    void setupRequiresAppMethod() {
        User user = userWithId();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        TwoFaSetupRequest req = new TwoFaSetupRequest();
        req.setMethod("sms");

        assertThrows(BadRequestException.class, () -> twoFaService.initiateSetup(user.getId(), req));
    }

    @Test
    void initiateSetupStoresPendingSecretAndReturnsPayload() {
        User user = userWithId();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        TotpService.SetupData setupData = TotpService.SetupData.builder()
                .secretKey("SECRET")
                .provisioningUri("otpauth://totp/Handshake:user@example.com?secret=SECRET")
                .qrCodeUrl("https://example.com/qr")
                .build();
        when(totpService.generateSetupData("Handshake", user.getEmail())).thenReturn(setupData);

        TwoFaSetupRequest req = new TwoFaSetupRequest();
        req.setMethod("app");

        TwoFaSetupResponse response = twoFaService.initiateSetup(user.getId(), req);

        assertEquals("SECRET", response.getSecretKey());
        assertEquals("https://example.com/qr", response.getQrCodeUrl());
        assertEquals("SECRET", user.getTwoFaSetupSecret());
        assertNotNull(user.getTwoFaSetupCreatedAt());
        verify(userRepository).save(user);
    }

    @Test
    void enableFailsWithoutPendingSetup() {
        User user = userWithId();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        TwoFaEnableRequest req = new TwoFaEnableRequest();
        req.setVerificationCode("123456");

        assertThrows(BadRequestException.class, () -> twoFaService.enable(user.getId(), req));
    }

    @Test
    void enableVerifiesCodeAndActivatesTwoFa() {
        User user = userWithId();
        user.setTwoFaSetupSecret("SECRET");
        user.setTwoFaSetupCreatedAt(LocalDateTime.now());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.verifyCode("SECRET", "123456")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "h-" + invocation.getArgument(0));

        TwoFaEnableRequest req = new TwoFaEnableRequest();
        req.setVerificationCode("123456");

        var recoveryCodes = twoFaService.enable(user.getId(), req);

        assertEquals(true, user.isTwoFaEnabled());
        assertEquals("app", user.getTwoFaMethod());
        assertEquals("SECRET", user.getTwoFaSecret());
        assertNotNull(user.getTwoFaEnabledAt());
        assertEquals(8, recoveryCodes.size());
        verify(userRepository).save(user);
    }

    private User userWithId() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user@example.com");
        u.setPasswordHash("hash");
        return u;
    }
}


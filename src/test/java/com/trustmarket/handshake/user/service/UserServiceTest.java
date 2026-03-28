package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void updateBasicsResetsPhoneVerificationWhenPhoneChanges() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("secret")
                .firstName("Old")
                .lastName("Name")
                .phone("+27111111111")
                .phoneVerified(true)
                .phoneVerifiedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateBasics(user.getId(), "New", "Name", "+27222222222", null);

        assertEquals("+27222222222", updated.getPhone());
        assertFalse(updated.isPhoneVerified());
        assertNull(updated.getPhoneVerifiedAt());
        verify(userRepository).save(user);
    }

    @Test
    void updateBasicsKeepsPhoneVerificationWhenPhoneIsUnchanged() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("secret")
                .firstName("Old")
                .lastName("Name")
                .phone("+27111111111")
                .phoneVerified(true)
                .phoneVerifiedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateBasics(user.getId(), null, null, " +27111111111 ", null);

        assertTrue(updated.isPhoneVerified());
        assertNotNull(updated.getPhoneVerifiedAt());
    }
}


package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.shared.exception.BadRequestException;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    @Transactional
    public User updateBasics(UUID userId, String firstName, String lastName, String phone, String profilePictureUrl) {
        User u = getById(userId);

        if (firstName != null) u.setFirstName(firstName.trim());
        if (lastName != null) u.setLastName(lastName.trim());
        if (phone != null) {
            String normalizedPhone = normalizeOptional(phone);
            if (!Objects.equals(normalizedPhone, u.getPhone())) {
                u.setPhone(normalizedPhone);
                u.setPhoneVerified(false);
                u.setPhoneVerifiedAt(null);
            }
        }
        if (profilePictureUrl != null) u.setProfilePictureUrl(profilePictureUrl.trim());

        return userRepository.save(u);
    }

    @Transactional
    public void recordLoginSuccess(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void recordLoginFailure(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        // Simple lock policy: lock 15 minutes after 5 failures
        if (attempts >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
        }
        userRepository.save(user);
    }

    public void assertNotLocked(User user) {
        if (user.isLockedNow()) {
            throw new BadRequestException("Account is locked until " + user.getAccountLockedUntil());
        }
    }

    private String normalizeOptional(String value) {
        String trimmed = value == null ? null : value.trim();
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
    }
}
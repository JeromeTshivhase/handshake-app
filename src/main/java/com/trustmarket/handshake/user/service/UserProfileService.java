package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.*;
import com.trustmarket.handshake.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Transactional
    public UserProfile getOrCreateProfile(UUID userId) {
        return userProfileRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + userId));
            UserProfile p = UserProfile.builder().user(user).build();
            return userProfileRepository.save(p);
        });
    }

    @Transactional
    public UserSettings getOrCreateSettings(UUID userId) {
        return userSettingsRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + userId));
            UserSettings s = UserSettings.builder().user(user).build();
            return userSettingsRepository.save(s);
        });
    }

    @Transactional
    public UserProfile updateProfile(UUID userId, String bio, String location, String city, String province) {
        UserProfile p = getOrCreateProfile(userId);
        if (bio != null) p.setBio(bio.trim());
        if (location != null) p.setLocation(location.trim());
        if (city != null) p.setCity(city.trim());
        if (province != null) p.setProvince(province.trim());
        return userProfileRepository.save(p);
    }

    @Transactional
    public UserSettings updateSettings(UUID userId, Boolean notificationsEnabled, Boolean emailNotifications, Boolean darkMode) {
        UserSettings s = getOrCreateSettings(userId);
        if (notificationsEnabled != null) s.setNotificationsEnabled(notificationsEnabled);
        if (emailNotifications != null) s.setEmailNotifications(emailNotifications);
        if (darkMode != null) s.setDarkMode(darkMode);
        return userSettingsRepository.save(s);
    }
}
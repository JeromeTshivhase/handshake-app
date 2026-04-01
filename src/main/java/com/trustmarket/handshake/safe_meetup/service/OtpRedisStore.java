package com.trustmarket.handshake.safe_meetup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpRedisStore {

    private final StringRedisTemplate redis;

    @Value("${app.redis.enabled:true}")
    private boolean redisEnabled;

    private String key(UUID handshakeId) {
        return "otp:handshake:" + handshakeId;
    }

    public void put(UUID handshakeId, String otp, Duration ttl) {
        if (!redisEnabled) {
            return;
        }
        try {
            redis.opsForValue().set(key(handshakeId), otp, ttl);
        } catch (DataAccessException ex) {
            // Keep handshake flow alive even when Redis is temporarily unavailable.
            log.warn("Redis unavailable while storing OTP for handshake {}: {}", handshakeId, ex.getMessage());
        }
    }

    public Optional<String> get(UUID handshakeId) {
        if (!redisEnabled) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(handshakeId)));
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while reading OTP for handshake {}: {}", handshakeId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void delete(UUID handshakeId) {
        if (!redisEnabled) {
            return;
        }
        try {
            redis.delete(key(handshakeId));
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while deleting OTP for handshake {}: {}", handshakeId, ex.getMessage());
        }
    }
}
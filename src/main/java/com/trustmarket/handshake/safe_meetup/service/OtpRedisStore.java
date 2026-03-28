package com.trustmarket.handshake.safe_meetup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpRedisStore {

    private final StringRedisTemplate redis;

    private String key(UUID handshakeId) {
        return "otp:handshake:" + handshakeId;
    }

    public void put(UUID handshakeId, String otp, Duration ttl) {
        redis.opsForValue().set(key(handshakeId), otp, ttl);
    }

    public Optional<String> get(UUID handshakeId) {
        return Optional.ofNullable(redis.opsForValue().get(key(handshakeId)));
    }

    public void delete(UUID handshakeId) {
        redis.delete(key(handshakeId));
    }
}
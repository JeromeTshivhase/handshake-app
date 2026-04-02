package com.trustmarket.handshake.shared.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 5 requests per minute per key (e.g., IP or email)
    private static final int REQUESTS = 5;
    private static final Duration DURATION = Duration.ofMinutes(1);

    public Bucket resolveBucket(String key) {
        return resolveBucket(key, REQUESTS, DURATION);
    }

    public Bucket resolveBucket(String key, int capacity, Duration period) {
        return buckets.computeIfAbsent(key, k -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, period)))
                .build());
    }
}


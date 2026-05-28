package com.trustmarket.handshake.shared.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Rate limiter backed by Redis so limits are enforced consistently across all
 * application instances.  Falls back to an in-process ConcurrentHashMap when
 * Redis is disabled (app.redis.enabled=false) so local / test environments
 * keep working without a Redis sidecar.
 */
@Slf4j
@Service
public class RateLimiterService {

    private static final int DEFAULT_REQUESTS = 5;
    private static final Duration DEFAULT_PERIOD = Duration.ofMinutes(1);

    private final boolean redisEnabled;
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    // Fallback for when Redis is disabled
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            RedisProperties redisProperties) {

        this.redisEnabled = redisEnabled;

        if (redisEnabled) {
            String host = redisProperties.getHost() != null ? redisProperties.getHost() : "localhost";
            int port = redisProperties.getPort() > 0 ? redisProperties.getPort() : 6379;
            String password = redisProperties.getPassword();

            String uri = (password != null && !password.isBlank())
                    ? String.format("redis://:%s@%s:%d", password, host, port)
                    : String.format("redis://%s:%d", host, port);

            RedisClient client = RedisClient.create(uri);
            StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
            this.proxyManager = LettuceBasedProxyManager.builderFor(connection).build();
            log.info("RateLimiterService: using Redis-backed rate limiting at {}:{}", host, port);
        } else {
            this.proxyManager = null;
            log.info("RateLimiterService: Redis disabled, using in-process rate limiting (single-instance only)");
        }
    }

    public Bucket resolveBucket(String key) {
        return resolveBucket(key, DEFAULT_REQUESTS, DEFAULT_PERIOD);
    }

    public Bucket resolveBucket(String key, int capacity, Duration period) {
        if (redisEnabled && proxyManager != null) {
            Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, period)))
                    .build();
            return proxyManager.builder().build(key.getBytes(), configSupplier);
        }
        // Local fallback
        return localBuckets.computeIfAbsent(key, k ->
                Bucket.builder()
                      .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, period)))
                      .build());
    }
}

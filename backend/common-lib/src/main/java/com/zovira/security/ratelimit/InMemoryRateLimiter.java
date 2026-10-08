package com.zovira.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single-node limiter backed by a bounded Caffeine cache. Suitable for development and
 * single-instance deployments; use {@link RedisRateLimiter} when running more than one replica.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .maximumSize(200_000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
    private final Clock clock;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Decision tryConsume(String key, int limit, Duration window) {
        long windowMillis = window.toMillis();
        long now = clock.millis();
        long windowStart = now - (now % windowMillis);
        AtomicInteger counter = counters.get(key + ':' + windowStart, k -> new AtomicInteger());
        int count = counter.incrementAndGet();
        long retryAfter = Math.max(1, (windowStart + windowMillis - now + 999) / 1000);
        return new Decision(count <= limit, limit, Math.max(0, limit - count), retryAfter);
    }
}

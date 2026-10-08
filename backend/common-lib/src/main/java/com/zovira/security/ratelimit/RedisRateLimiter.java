package com.zovira.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Distributed fixed-window limiter. Increment and expiry run atomically in a Lua script so a
 * crash between the two can never leave a key without a TTL. Fails open if Redis is unreachable
 * so an infrastructure outage does not take the whole API down.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final RedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public RedisRateLimiter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @Override
    public Decision tryConsume(String key, int limit, Duration window) {
        long windowMillis = window.toMillis();
        long now = clock.millis();
        long windowStart = now - (now % windowMillis);
        long retryAfter = Math.max(1, (windowStart + windowMillis - now + 999) / 1000);
        try {
            Long count = redis.execute(INCREMENT, List.of("rl:" + key + ':' + windowStart),
                    String.valueOf(windowMillis));
            int current = count == null ? 0 : count.intValue();
            return new Decision(current <= limit, limit, Math.max(0, limit - current), retryAfter);
        } catch (RuntimeException e) {
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
            return new Decision(true, limit, limit, 0);
        }
    }
}

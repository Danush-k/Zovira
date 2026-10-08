package com.zovira.security.ratelimit;

import java.time.Duration;

/** Fixed-window request counter. Implementations must be safe for concurrent use. */
public interface RateLimiter {

    record Decision(boolean allowed, int limit, int remaining, long retryAfterSeconds) {
    }

    Decision tryConsume(String key, int limit, Duration window);
}

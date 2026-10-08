package com.zovira.security.ratelimit;

import com.zovira.security.ProblemResponseWriter;
import com.zovira.security.SecurityProperties;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfig {

    @Bean
    RateLimiter rateLimiter(SecurityProperties properties, @Value("${zovira.redis.enabled:false}") boolean redisEnabled,
            ObjectProvider<StringRedisTemplate> redis, Clock clock) {
        if (!properties.rateLimit().enabled()) {
            return (key, limit, window) -> new RateLimiter.Decision(true, limit, limit, 0);
        }
        return redisEnabled
                ? new RedisRateLimiter(redis.getObject(), clock)
                : new InMemoryRateLimiter(clock);
    }

    @Bean
    RateLimitFilter rateLimitFilter(RateLimiter rateLimiter, ProblemResponseWriter problemWriter) {
        return new RateLimitFilter(rateLimiter, problemWriter);
    }

    /** The filter runs inside the security chain; stop Boot from also registering it globally. */
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}

package com.zovira.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableMongoAuditing
@EnableAsync
@EnableScheduling
public class CoreConfig {

    /** Injectable clock so time-dependent rules (coupons, token expiry, return windows) are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

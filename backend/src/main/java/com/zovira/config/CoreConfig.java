package com.zovira.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class CoreConfig {

    /** Injectable clock so time-dependent rules (coupons, token expiry, return windows) are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

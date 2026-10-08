package com.zovira.auth.service;

import com.zovira.auth.repository.RefreshTokenRepository;
import com.zovira.auth.repository.UserTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Purges expired refresh and account tokens nightly so the token tables stay small. */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokens;
    private final UserTokenRepository userTokens;
    private final Clock clock;

    public TokenCleanupJob(RefreshTokenRepository refreshTokens, UserTokenRepository userTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.userTokens = userTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "${zovira.jobs.token-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(7));
        int refresh = refreshTokens.deleteExpiredBefore(cutoff);
        int account = userTokens.deleteExpiredBefore(cutoff);
        log.info("Purged {} refresh tokens and {} account tokens", refresh, account);
    }
}

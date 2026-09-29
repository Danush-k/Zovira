package com.zovira.seed;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Populates an empty database with demo users, catalog, reviews, coupons and orders so a fresh
 * environment is immediately explorable. Enabled with {@code SEED_ENABLED=true} (the dev default);
 * never enable it in production.
 */
@Component
@ConditionalOnProperty(prefix = "zovira.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final List<SeedStep> steps;
    private final TransactionTemplate transactions;

    public DataSeeder(List<SeedStep> steps, TransactionTemplate transactions) {
        this.steps = steps;
        this.transactions = transactions;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedStep step : steps) {
            if (!step.shouldRun()) {
                log.debug("Seed step '{}' already applied", step.name());
                continue;
            }
            long start = System.currentTimeMillis();
            transactions.executeWithoutResult(status -> step.run());
            log.info("Seed step '{}' completed in {} ms", step.name(), System.currentTimeMillis() - start);
        }
    }
}

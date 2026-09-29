package com.zovira.seed;

/** One idempotent unit of demo data. Steps run in {@code @Order} sequence, each in its own transaction. */
public interface SeedStep {

    String name();

    /** Whether this step still has work to do (for example, its tables are empty). */
    boolean shouldRun();

    void run();
}

package com.zovira.catalog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Buffers product page views in memory and flushes them in one batch every 30 seconds, so busy
 * product pages don't turn every read into a row lock on {@code products}.
 */
@Component
public class ProductViewCounter {

    private final Map<Long, LongAdder> pending = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;

    public ProductViewCounter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void increment(Long productId) {
        pending.computeIfAbsent(productId, id -> new LongAdder()).increment();
    }

    @Scheduled(fixedDelayString = "${zovira.jobs.view-flush-ms:30000}")
    public void flush() {
        if (pending.isEmpty()) {
            return;
        }
        List<Object[]> batch = new ArrayList<>();
        pending.forEach((id, adder) -> {
            long count = adder.sumThenReset();
            if (count > 0) {
                batch.add(new Object[] {count, id});
            }
        });
        if (!batch.isEmpty()) {
            jdbc.batchUpdate("UPDATE products SET view_count = view_count + ? WHERE id = ?", batch);
        }
    }
}

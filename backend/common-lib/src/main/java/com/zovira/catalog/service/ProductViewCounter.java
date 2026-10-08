package com.zovira.catalog.service;

import com.zovira.catalog.entity.Product;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ProductViewCounter {

    private final Map<Long, LongAdder> pending = new ConcurrentHashMap<>();
    private final MongoTemplate mongoTemplate;

    public ProductViewCounter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void increment(Long productId) {
        pending.computeIfAbsent(productId, id -> new LongAdder()).increment();
    }

    @Scheduled(fixedDelayString = "${zovira.jobs.view-flush-ms:30000}")
    public void flush() {
        if (pending.isEmpty()) {
            return;
        }
        pending.forEach((id, adder) -> {
            long count = adder.sumThenReset();
            if (count > 0) {
                mongoTemplate.updateFirst(
                        new Query(Criteria.where("_id").is(id)),
                        new Update().inc("viewCount", count),
                        Product.class);
            }
        });
    }
}

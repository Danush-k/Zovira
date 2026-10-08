package com.zovira.coupon.repository;

import com.zovira.coupon.entity.Coupon;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class CouponRepositoryImpl implements CouponRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public CouponRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Coupon> findLive(Instant now) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("active").is(true),
                Criteria.where("startsAt").lte(now),
                new Criteria().orOperator(
                        Criteria.where("expiresAt").is(null),
                        Criteria.where("expiresAt").gt(now)
                )
        );
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "discountValue"));
        return mongoTemplate.find(query, Coupon.class);
    }
}

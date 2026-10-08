package com.zovira.order.repository;

import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public OrderRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Order> findForUser(Long userId, String status, Collection<OrderStatus> statuses, Pageable pageable) {
        Criteria criteria = Criteria.where("userId").is(userId);
        if (statuses != null && !statuses.isEmpty()) {
            criteria.and("status").in(statuses);
        }

        Query countQuery = new Query(criteria);
        long total = mongoTemplate.count(countQuery, Order.class);

        Query query = new Query(criteria).with(pageable);
        List<Order> content = mongoTemplate.find(query, Order.class);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Long> findIdsByStatusPlacedBefore(OrderStatus status, Instant before) {
        Query query = new Query(Criteria.where("status").is(status).and("placedAt").lt(before));
        query.fields().include("_id");
        List<Order> list = mongoTemplate.find(query, Order.class);
        return list.stream().map(Order::getId).toList();
    }

    @Override
    public boolean hasPurchased(Long userId, Long productId, OrderStatus status) {
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("status").is(status)
                .and("items.productId").is(productId));
        return mongoTemplate.exists(query, Order.class);
    }
}

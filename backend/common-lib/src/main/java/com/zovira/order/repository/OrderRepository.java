package com.zovira.order.repository;

import com.zovira.order.entity.Order;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, Long>, OrderRepositoryCustom {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}

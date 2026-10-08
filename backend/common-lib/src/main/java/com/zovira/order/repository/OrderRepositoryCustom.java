package com.zovira.order.repository;

import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepositoryCustom {

    Page<Order> findForUser(Long userId, String status, Collection<OrderStatus> statuses, Pageable pageable);

    List<Long> findIdsByStatusPlacedBefore(OrderStatus status, Instant before);

    boolean hasPurchased(Long userId, Long productId, OrderStatus status);
}

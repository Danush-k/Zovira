package com.zovira.order.repository;

import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("select o from Order o where o.user.id = :userId and (:status is null or o.status in :statuses) order by o.placedAt desc")
    Page<Order> findForUser(@Param("userId") Long userId, @Param("status") String status,
            @Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);

    @Query("select o.id from Order o where o.status = :status and o.placedAt < :before")
    List<Long> findIdsByStatusPlacedBefore(@Param("status") OrderStatus status, @Param("before") Instant before);

    @Query("""
            select count(oi) > 0 from OrderItem oi
            where oi.order.user.id = :userId and oi.product.id = :productId and oi.order.status = :status
            """)
    boolean hasPurchased(@Param("userId") Long userId, @Param("productId") Long productId,
            @Param("status") OrderStatus status);
}

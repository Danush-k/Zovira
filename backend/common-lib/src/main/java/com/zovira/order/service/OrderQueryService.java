package com.zovira.order.service;

import com.zovira.common.exception.NotFoundException;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.order.dto.OrderDetailResponse;
import com.zovira.order.dto.OrderSummaryResponse;
import com.zovira.order.entity.OrderStatus;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import com.zovira.order.mapper.OrderMapper;
import com.zovira.order.repository.OrderRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository orders;
    private final Clock clock;

    public OrderQueryService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    /** The customer's orders, newest first, optionally narrowed to active, delivered or cancelled. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(Long userId, String filter, int page, int size) {
        Set<OrderStatus> statuses = switch (filter == null ? "all" : filter.toLowerCase(Locale.ROOT)) {
            case "active" -> EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PLACED, OrderStatus.CONFIRMED,
                    OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY);
            case "delivered" -> EnumSet.of(OrderStatus.DELIVERED, OrderStatus.RETURNED);
            case "cancelled" -> EnumSet.of(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED);
            default -> null;
        };
        return PageResponse.of(orders.findForUser(userId, statuses == null ? null : "x",
                statuses == null ? EnumSet.allOf(OrderStatus.class) : statuses, PageRequests.of(page, size)),
                OrderMapper::toSummary);
    }

    /** Order detail, only for its owner. Unknown and foreign order numbers look identical (404). */
    @Transactional(readOnly = true)
    public OrderDetailResponse detail(Long userId, String orderNumber) {
        return orders.findByOrderNumberAndUserId(orderNumber, userId)
                .map(o -> OrderMapper.toDetail(o, clock.instant()))
                .orElseThrow(() -> NotFoundException.of("Order"));
    }
}

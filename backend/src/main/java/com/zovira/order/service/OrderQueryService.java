package com.zovira.order.service;

import com.zovira.common.exception.NotFoundException;
import com.zovira.order.dto.OrderDetailResponse;
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

    /** Order detail, only for its owner. Unknown and foreign order numbers look identical (404). */
    @Transactional(readOnly = true)
    public OrderDetailResponse detail(Long userId, String orderNumber) {
        return orders.findByOrderNumberAndUserId(orderNumber, userId)
                .map(o -> OrderMapper.toDetail(o, clock.instant()))
                .orElseThrow(() -> NotFoundException.of("Order"));
    }
}

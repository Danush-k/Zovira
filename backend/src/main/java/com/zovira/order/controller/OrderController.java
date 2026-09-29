package com.zovira.order.controller;

import com.zovira.order.dto.OrderDetailResponse;
import com.zovira.order.service.OrderQueryService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "The signed-in customer's orders")
public class OrderController {

    private final OrderQueryService queryService;

    public OrderController(OrderQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Order details, shipments and status timeline")
    public OrderDetailResponse detail(@CurrentUser AuthUser user, @PathVariable String orderNumber) {
        return queryService.detail(user.id(), orderNumber);
    }
}

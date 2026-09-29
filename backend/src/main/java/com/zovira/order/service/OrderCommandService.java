package com.zovira.order.service;

import com.zovira.cart.dto.CartLineRequest;
import com.zovira.cart.service.CartService;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.coupon.service.CouponLedger;
import com.zovira.inventory.service.InventoryService;
import com.zovira.order.dto.OrderDetailResponse;
import com.zovira.order.dto.ReorderResponse;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.event.OrderEvent;
import com.zovira.order.mapper.OrderMapper;
import com.zovira.order.repository.OrderRepository;
import com.zovira.payment.service.RefundService;
import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCommandService {

    private final OrderRepository orders;
    private final InventoryService inventory;
    private final CouponLedger couponLedger;
    private final RefundService refundService;
    private final CartService cartService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderCommandService(OrderRepository orders, InventoryService inventory, CouponLedger couponLedger,
            RefundService refundService, CartService cartService, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.inventory = inventory;
        this.couponLedger = couponLedger;
        this.refundService = refundService;
        this.cartService = cartService;
        this.events = events;
        this.clock = clock;
    }

    /** Customer cancellation, allowed until any package has shipped. */
    @Transactional
    public OrderDetailResponse cancelByCustomer(Long userId, String orderNumber, String reason) {
        Order order = owned(userId, orderNumber);
        cancel(order, "Cancelled by customer: " + reason.trim(), userId);
        return OrderMapper.toDetail(order, clock.instant());
    }

    /** Shared by customers, sellers (whole-order cancellation) and admins. */
    @Transactional
    public void cancel(Order order, String reason, Long actorId) {
        if (!order.getStatus().isCancellable()
                || order.getShipments().stream().anyMatch(s -> s.getStatus().rank() >= ShipmentStatus.SHIPPED.rank())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "This order has already shipped and can't be cancelled. You can return it after delivery.");
        }
        Instant now = clock.instant();
        boolean reserved = order.getStatus() == OrderStatus.PENDING_PAYMENT;
        order.transitionTo(OrderStatus.CANCELLED, reason, actorId, now);
        List<OrderItem> active = order.getItems().stream().filter(i -> i.getStatus() == OrderItemStatus.ACTIVE).toList();
        if (reserved) {
            inventory.release(active);
        } else {
            active.forEach(i -> inventory.restock(i, i.getQuantity()));
        }
        active.forEach(i -> i.setStatus(OrderItemStatus.CANCELLED));
        for (Shipment s : order.getShipments()) {
            if (s.getStatus().canAdvanceTo(ShipmentStatus.CANCELLED)) {
                s.advance(ShipmentStatus.CANCELLED, null, "Order cancelled", now);
            }
        }
        couponLedger.reverse(order);
        refundService.refund(order, order.getTotalAmount(), "Order cancelled", null);
        events.publishEvent(new OrderEvent(order.getId(), order.getUser().getId(), order.getOrderNumber(),
                OrderEvent.Type.CANCELLED, reason));
    }

    /** Adds every still-available item from a past order back into the cart. */
    @Transactional
    public ReorderResponse reorder(Long userId, String orderNumber) {
        Order order = owned(userId, orderNumber);
        int added = 0;
        List<String> unavailable = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            try {
                cartService.add(userId, new CartLineRequest(item.getVariant().getId(), Math.min(item.getQuantity(), 10)));
                added++;
            } catch (ApiException e) {
                unavailable.add(item.getProductTitle());
            }
        }
        return new ReorderResponse(added, unavailable);
    }

    Order owned(Long userId, String orderNumber) {
        return orders.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() -> NotFoundException.of("Order"));
    }
}

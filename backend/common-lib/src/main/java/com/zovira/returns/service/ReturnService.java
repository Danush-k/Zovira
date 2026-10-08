package com.zovira.returns.service;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.common.util.SecureTokens;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.inventory.service.InventoryService;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.event.OrderEvent;
import com.zovira.order.mapper.OrderMapper;
import com.zovira.order.repository.OrderRepository;
import com.zovira.payment.service.RefundService;
import com.zovira.returns.dto.CreateReturnRequest;
import com.zovira.returns.dto.ReturnDecisionRequest;
import com.zovira.returns.dto.ReturnResponse;
import com.zovira.returns.entity.ReturnRequest;
import com.zovira.returns.entity.ReturnStatus;
import com.zovira.returns.repository.ReturnRequestRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Return lifecycle: the customer requests within the product's return window, the seller (or an
 * admin) approves or rejects, and receiving the item restocks it and refunds the customer the
 * amount they actually paid for it (after its share of any coupon).
 */
@Service
public class ReturnService {

    private final ReturnRequestRepository returns;
    private final OrderRepository orders;
    private final InventoryService inventory;
    private final RefundService refunds;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReturnService(ReturnRequestRepository returns, OrderRepository orders, InventoryService inventory,
            RefundService refunds, ApplicationEventPublisher events, Clock clock) {
        this.returns = returns;
        this.orders = orders;
        this.inventory = inventory;
        this.refunds = refunds;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ReturnResponse request(Long userId, String orderNumber, Long itemId, CreateReturnRequest body) {
        Order order = orders.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() -> NotFoundException.of("Order"));
        OrderItem item = order.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Order item"));
        if (order.getStatus() != OrderStatus.DELIVERED || item.getStatus() != OrderItemStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "This item can't be returned");
        }
        LocalDate deadline = OrderMapper.returnDeadline(order, item);
        if (deadline == null) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "This item isn't eligible for return");
        }
        if (LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata"))).isAfter(deadline)) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "The return window for this item has closed");
        }
        if (body.quantity() > item.getQuantity()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "You can return at most " + item.getQuantity());
        }
        BigDecimal perUnit = item.netAmount().divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
        ReturnRequest request = new ReturnRequest(SecureTokens.reference("RT", 10), item, body.quantity(), body.reason(),
                body.comments() == null || body.comments().isBlank() ? null : body.comments().trim(),
                perUnit.multiply(BigDecimal.valueOf(body.quantity())));
        item.setStatus(OrderItemStatus.RETURN_REQUESTED);
        returns.save(request);
        publish(order, "Return " + request.getReturnNumber() + " requested");
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> mine(Long userId, Integer page) {
        return PageResponse.of(returns.findByUserIdOrderByCreatedAtDesc(userId, PageRequests.of(page, 20)),
                ReturnService::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> forSeller(Long sellerId, boolean openOnly, Integer page) {
        Set<ReturnStatus> statuses = openOnly ? EnumSet.of(ReturnStatus.REQUESTED, ReturnStatus.APPROVED)
                : EnumSet.allOf(ReturnStatus.class);
        return PageResponse.of(returns.findForSeller(sellerId, statuses, PageRequests.of(page, 20)), ReturnService::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> all(boolean openOnly, Integer page) {
        Set<ReturnStatus> statuses = openOnly ? EnumSet.of(ReturnStatus.REQUESTED, ReturnStatus.APPROVED)
                : EnumSet.allOf(ReturnStatus.class);
        return PageResponse.of(returns.findByStatuses(statuses, PageRequests.of(page, 20)), ReturnService::toResponse);
    }

    /** Seller decision; {@code sellerId} null means an admin acting on any return. */
    @Transactional
    public ReturnResponse decide(Long returnId, Long sellerId, Long actorId, ReturnDecisionRequest decision) {
        ReturnRequest request = returns.findById(returnId)
                .filter(r -> sellerId == null || r.getSeller().getId().equals(sellerId))
                .orElseThrow(() -> NotFoundException.of("Return"));
        var now = clock.instant();
        switch (decision.decision()) {
            case APPROVE -> request.approve(actorId, decision.note(), now);
            case REJECT -> {
                request.reject(actorId, decision.note() == null ? "Return rejected" : decision.note(), now);
                request.getOrderItem().setStatus(OrderItemStatus.ACTIVE);
            }
            case RECEIVE -> receive(request, actorId);
        }
        publish(request.getOrder(), "Return " + request.getReturnNumber() + " " + request.getStatus().name().toLowerCase());
        return toResponse(request);
    }

    private void receive(ReturnRequest request, Long actorId) {
        var now = clock.instant();
        request.markPickedUp(actorId, now);
        OrderItem item = request.getOrderItem();
        inventory.restock(item, request.getQuantity());
        item.setStatus(OrderItemStatus.RETURNED);
        refunds.refund(request.getOrder(), request.getRefundAmount(), "Return " + request.getReturnNumber(), request);
        request.markRefunded(now);
        Order order = request.getOrder();
        boolean everythingReturned = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.RETURNED || i.getStatus() == OrderItemStatus.CANCELLED);
        if (everythingReturned && order.getStatus().canTransitionTo(OrderStatus.RETURNED)) {
            order.transitionTo(OrderStatus.RETURNED, "All items returned", actorId, now);
        }
    }

    private void publish(Order order, String note) {
        events.publishEvent(new OrderEvent(order.getId(), order.getUser().getId(), order.getOrderNumber(),
                OrderEvent.Type.RETURN_UPDATED, note));
    }

    public static ReturnResponse toResponse(ReturnRequest r) {
        OrderItem i = r.getOrderItem();
        return new ReturnResponse(r.getId(), r.getReturnNumber(), r.getOrder().getOrderNumber(), i.getProductTitle(),
                i.getImageUrl(), i.getProduct().getSlug(), r.getQuantity(), r.getReason().name(), r.getComments(),
                r.getStatus().name(), r.getRefundAmount(), r.getResolutionNote(), r.getUser().getFullName(),
                r.getSeller().getStoreName(), r.getCreatedAt(), r.getResolvedAt());
    }
}

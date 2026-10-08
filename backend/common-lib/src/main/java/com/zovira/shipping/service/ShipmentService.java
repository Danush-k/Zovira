package com.zovira.shipping.service;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.PaymentMethod;
import com.zovira.order.entity.PaymentStatus;
import com.zovira.order.event.OrderEvent;
import com.zovira.payment.entity.PaymentTransactionStatus;
import com.zovira.payment.repository.PaymentRepository;
import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a seller's shipment through fulfilment and keeps the parent order in step: the order
 * reflects its least-advanced active shipment, so it only shows "Delivered" once every seller's
 * package has arrived.
 */
@Service
public class ShipmentService {

    private static final Map<ShipmentStatus, String> DEFAULT_NOTES = Map.of(
            ShipmentStatus.PROCESSING, "Seller is packing your order",
            ShipmentStatus.SHIPPED, "Package handed over to the courier",
            ShipmentStatus.OUT_FOR_DELIVERY, "Out for delivery with our delivery partner",
            ShipmentStatus.DELIVERED, "Delivered",
            ShipmentStatus.CANCELLED, "Shipment cancelled");

    private final PaymentRepository payments;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ShipmentService(PaymentRepository payments, ApplicationEventPublisher events, Clock clock) {
        this.payments = payments;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void advance(Shipment shipment, ShipmentStatus target, String carrier, String trackingNumber,
            String location, String note, Long actorId) {
        Order order = shipment.getOrder();
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus().isTerminal()) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "This order can't be fulfilled in its current state");
        }
        if (target == ShipmentStatus.SHIPPED && (isBlank(carrier) || isBlank(trackingNumber))
                && (isBlank(shipment.getCarrier()) || isBlank(shipment.getTrackingNumber()))) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Carrier and tracking number are required to ship");
        }
        if (!isBlank(carrier)) {
            shipment.setCarrier(carrier.trim());
        }
        if (!isBlank(trackingNumber)) {
            shipment.setTrackingNumber(trackingNumber.trim());
        }
        Instant now = clock.instant();
        shipment.advance(target, location, isBlank(note) ? DEFAULT_NOTES.get(target) : note.trim(), now);
        syncOrder(order, actorId, now);
    }

    private void syncOrder(Order order, Long actorId, Instant now) {
        ShipmentStatus slowest = order.getShipments().stream()
                .map(Shipment::getStatus)
                .filter(s -> s != ShipmentStatus.CANCELLED)
                .min(Comparator.comparingInt(ShipmentStatus::rank))
                .orElse(null);
        if (slowest == null) {
            return;
        }
        OrderStatus target = slowest.orderStatus();
        if (!order.getStatus().canTransitionTo(target)) {
            return;
        }
        order.transitionTo(target, null, actorId, now);
        if (target == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
            // Cash collected at the door.
            order.setPaymentStatus(PaymentStatus.PAID);
            payments.findByOrderIdOrderByIdDesc(order.getId()).stream().findFirst()
                    .filter(p -> p.getStatus() == PaymentTransactionStatus.CREATED)
                    .ifPresent(p -> p.markCaptured("COD-" + order.getOrderNumber(), now));
        }
        OrderEvent.Type type = switch (target) {
            case PROCESSING -> OrderEvent.Type.PROCESSING;
            case SHIPPED -> OrderEvent.Type.SHIPPED;
            case OUT_FOR_DELIVERY -> OrderEvent.Type.OUT_FOR_DELIVERY;
            case DELIVERED -> OrderEvent.Type.DELIVERED;
            default -> null;
        };
        if (type != null) {
            events.publishEvent(new OrderEvent(order.getId(), order.getUser().getId(), order.getOrderNumber(), type, null));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

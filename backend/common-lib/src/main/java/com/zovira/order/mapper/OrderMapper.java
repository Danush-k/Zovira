package com.zovira.order.mapper;

import com.zovira.order.dto.OrderDetailResponse;
import com.zovira.order.dto.OrderItemResponse;
import com.zovira.order.dto.OrderSummaryResponse;
import com.zovira.order.dto.ShipmentResponse;
import com.zovira.order.dto.TimelineEntry;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.PaymentStatus;
import com.zovira.order.entity.ShippingAddress;
import com.zovira.shipping.entity.Shipment;
import com.zovira.user.dto.AddressResponse;
import com.zovira.user.entity.AddressType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public final class OrderMapper {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public static final Map<OrderStatus, String> LABELS = Map.of(
            OrderStatus.PENDING_PAYMENT, "Awaiting payment",
            OrderStatus.PLACED, "Order placed",
            OrderStatus.CONFIRMED, "Order confirmed",
            OrderStatus.PROCESSING, "Seller is preparing your package",
            OrderStatus.SHIPPED, "Shipped",
            OrderStatus.OUT_FOR_DELIVERY, "Out for delivery",
            OrderStatus.DELIVERED, "Delivered",
            OrderStatus.CANCELLED, "Cancelled",
            OrderStatus.PAYMENT_FAILED, "Payment not completed",
            OrderStatus.RETURNED, "Returned");

    private OrderMapper() {
    }

    public static OrderSummaryResponse toSummary(Order o) {
        return new OrderSummaryResponse(o.getOrderNumber(), o.getStatus().name(), o.getPaymentStatus().name(),
                o.getPaymentMethod().name(), o.getTotalAmount(), o.getItemCount(), o.getPlacedAt(),
                o.getEstimatedDeliveryDate(), o.getDeliveredAt(),
                o.getItems().stream().map(OrderItem::getImageUrl).filter(u -> u != null).limit(4).toList(),
                o.getItems().isEmpty() ? null : o.getItems().getFirst().getProductTitle());
    }

    public static OrderDetailResponse toDetail(Order o, Instant now) {
        return new OrderDetailResponse(o.getOrderNumber(), o.getStatus().name(), o.getPaymentStatus().name(),
                o.getPaymentMethod().name(), o.getDeliveryOption().name(), address(o.getShippingAddress()),
                o.getItems().stream().map(i -> item(o, i, now)).toList(),
                o.getShipments().stream().map(OrderMapper::shipment).toList(),
                o.getStatusHistory().stream()
                        .map(h -> new TimelineEntry(h.getStatus().name(), LABELS.get(h.getStatus()), h.getNote(),
                                h.getCreatedAt()))
                        .toList(),
                o.getMrpTotal(), o.getSubtotal(), o.getCouponCode(), o.getCouponDiscount(), o.getShippingFee(),
                o.getCodFee(), o.getTaxAmount(), o.getTotalAmount(), o.getRefundedAmount(),
                o.getEstimatedDeliveryDate(), o.getPlacedAt(), o.getDeliveredAt(), o.getCancelledAt(),
                o.getCancelReason(), o.getStatus().isCancellable(),
                o.getStatus() == OrderStatus.PENDING_PAYMENT && o.getPaymentStatus() != PaymentStatus.PAID);
    }

    public static ShipmentResponse shipment(Shipment s) {
        return new ShipmentResponse(s.getId(), s.getShipmentNumber(), s.getStatus().name(), s.getSeller().getStoreName(),
                s.getCarrier(), s.getTrackingNumber(), s.getEstimatedDeliveryDate(), s.getShippedAt(),
                s.getDeliveredAt(),
                s.getEvents().stream().map(e -> new ShipmentResponse.Event(e.getStatus().name(), e.getLocation(),
                        e.getDescription(), e.getOccurredAt())).toList(),
                s.getItems().stream().map(OrderItem::getId).toList());
    }

    /** Last day a delivered item can be returned, or null if it never can be. */
    public static LocalDate returnDeadline(Order o, OrderItem i) {
        if (o.getDeliveredAt() == null || !i.getProduct().isReturnable() || i.getProduct().getReturnWindowDays() <= 0) {
            return null;
        }
        return o.getDeliveredAt().atZone(IST).toLocalDate().plusDays(i.getProduct().getReturnWindowDays());
    }

    private static OrderItemResponse item(Order o, OrderItem i, Instant now) {
        LocalDate deadline = returnDeadline(o, i);
        boolean returnable = o.getStatus() == OrderStatus.DELIVERED && i.getStatus() == OrderItemStatus.ACTIVE
                && deadline != null && !now.atZone(IST).toLocalDate().isAfter(deadline);
        return new OrderItemResponse(i.getId(), i.getProduct().getId(), i.getProduct().getSlug(), i.getProductTitle(),
                i.getVariantName(), i.getSku(), i.getImageUrl(), i.getUnitPrice(), i.getUnitMrp(), i.getQuantity(),
                i.getLineTotal(), i.getCouponDiscount(), i.getStatus().name(), i.getSeller().getStoreName(),
                i.getShipment() == null ? null : i.getShipment().getShipmentNumber(), returnable,
                deadline == null ? null : deadline.toString(), o.getStatus() == OrderStatus.DELIVERED);
    }

    private static AddressResponse address(ShippingAddress a) {
        return new AddressResponse(null, a.getName(), a.getPhone(), a.getLine1(), a.getLine2(), a.getLandmark(),
                a.getCity(), a.getState(), a.getPincode(), a.getCountry(), AddressType.HOME, false);
    }
}

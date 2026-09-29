package com.zovira.seller.service;

import com.zovira.common.exception.NotFoundException;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.seller.dto.SellerOrderRow;
import com.zovira.seller.dto.ShipmentUpdateRequest;
import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentStatus;
import com.zovira.shipping.repository.ShipmentRepository;
import com.zovira.shipping.service.ShipmentService;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A seller's fulfilment queue: their shipments only, never another seller's items. */
@Service
public class SellerOrderService {

    private final ShipmentRepository shipments;
    private final ShipmentService shipmentService;

    public SellerOrderService(ShipmentRepository shipments, ShipmentService shipmentService) {
        this.shipments = shipments;
        this.shipmentService = shipmentService;
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerOrderRow> list(Long sellerId, String filter, Integer page, Integer size) {
        Set<ShipmentStatus> statuses = switch (filter == null ? "open" : filter.toLowerCase(Locale.ROOT)) {
            case "pending" -> EnumSet.of(ShipmentStatus.PENDING);
            case "processing" -> EnumSet.of(ShipmentStatus.PROCESSING);
            case "shipped" -> EnumSet.of(ShipmentStatus.SHIPPED, ShipmentStatus.OUT_FOR_DELIVERY);
            case "delivered" -> EnumSet.of(ShipmentStatus.DELIVERED);
            case "cancelled" -> EnumSet.of(ShipmentStatus.CANCELLED);
            case "all" -> EnumSet.allOf(ShipmentStatus.class);
            default -> EnumSet.of(ShipmentStatus.PENDING, ShipmentStatus.PROCESSING, ShipmentStatus.SHIPPED,
                    ShipmentStatus.OUT_FOR_DELIVERY);
        };
        Page<Shipment> found = shipments.findForSeller(sellerId, statuses, PageRequests.of(page, size));
        return PageResponse.of(found, SellerOrderService::toRow);
    }

    @Transactional
    public SellerOrderRow updateStatus(Long sellerId, Long userId, Long shipmentId, ShipmentUpdateRequest request) {
        Shipment shipment = shipments.findById(shipmentId)
                .filter(s -> s.getSeller().getId().equals(sellerId))
                .orElseThrow(() -> NotFoundException.of("Shipment"));
        shipmentService.advance(shipment, request.status(), request.carrier(), request.trackingNumber(),
                request.location(), request.note(), userId);
        return toRow(shipment);
    }

    @Transactional(readOnly = true)
    public long countPending(Long sellerId) {
        return shipments.countBySellerIdAndStatusIn(sellerId,
                EnumSet.of(ShipmentStatus.PENDING, ShipmentStatus.PROCESSING));
    }

    static SellerOrderRow toRow(Shipment s) {
        Order order = s.getOrder();
        List<OrderItem> items = s.getItems();
        BigDecimal total = items.stream().map(OrderItem::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SellerOrderRow(s.getId(), s.getShipmentNumber(), order.getOrderNumber(), s.getStatus().name(),
                order.getStatus().name(), order.getPaymentMethod().name(), order.getPaymentStatus().name(),
                order.getShippingAddress().getName(), order.getShippingAddress().getCity(),
                order.getShippingAddress().getState(), order.getShippingAddress().getPincode(),
                s.getEstimatedDeliveryDate(), s.getCarrier(), s.getTrackingNumber(), order.getPlacedAt(), total,
                items.stream().map(i -> new SellerOrderRow.Item(i.getId(), i.getProductTitle(), i.getVariantName(),
                        i.getSku(), i.getImageUrl(), i.getQuantity(), i.getLineTotal(), i.getStatus().name())).toList());
    }
}

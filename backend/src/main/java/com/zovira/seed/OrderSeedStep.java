package com.zovira.seed;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.util.SecureTokens;
import com.zovira.order.entity.DeliveryOption;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.PaymentMethod;
import com.zovira.order.entity.PaymentStatus;
import com.zovira.order.entity.ShippingAddress;
import com.zovira.order.repository.OrderRepository;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import com.zovira.payment.repository.PaymentRepository;
import com.zovira.returns.entity.ReturnReason;
import com.zovira.returns.entity.ReturnRequest;
import com.zovira.returns.repository.ReturnRequestRepository;
import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentStatus;
import com.zovira.shipping.service.ShipmentService;
import com.zovira.user.entity.Address;
import com.zovira.user.entity.User;
import com.zovira.user.repository.AddressRepository;
import com.zovira.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Historical orders across 90 days in every lifecycle state, so order history, tracking, seller
 * dashboards and admin analytics have realistic data. Reviews backed by a delivered order are
 * marked as verified purchases.
 */
@Component
@org.springframework.core.annotation.Order(30)
class OrderSeedStep implements SeedStep {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final String[] CARRIERS = {"BlueDart", "Delhivery", "Ekart", "DTDC", "Xpressbees"};

    private final OrderRepository orders;
    private final UserRepository users;
    private final AddressRepository addresses;
    private final ProductRepository products;
    private final PaymentRepository payments;
    private final ReturnRequestRepository returns;
    private final ShipmentService shipments;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    OrderSeedStep(OrderRepository orders, UserRepository users, AddressRepository addresses, ProductRepository products,
            PaymentRepository payments, ReturnRequestRepository returns, ShipmentService shipments, JdbcTemplate jdbc,
            Clock clock) {
        this.orders = orders;
        this.users = users;
        this.addresses = addresses;
        this.products = products;
        this.payments = payments;
        this.returns = returns;
        this.shipments = shipments;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "orders";
    }

    @Override
    public boolean shouldRun() {
        return orders.count() == 0 && users.count() > 0;
    }

    @Override
    public void run() {
        Random random = new Random(7);
        Instant now = clock.instant();
        List<Product> catalog = products.findAll(PageRequest.of(0, 500, Sort.by("id"))).getContent().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.defaultVariant() != null)
                .toList();
        List<User> customers = users.findAll(Sort.by("id")).stream()
                .filter(u -> u.getEmail().endsWith("@zovira.test") && !u.getEmail().startsWith("seller.")
                        && !u.getEmail().startsWith("admin"))
                .filter(u -> !addresses.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(u.getId()).isEmpty())
                .toList();

        List<Object[]> backdates = new ArrayList<>();
        int n = 0;
        for (User customer : customers) {
            Address address = addresses.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(customer.getId()).getFirst();
            List<Long> reviewed = jdbc.queryForList("SELECT product_id FROM reviews WHERE user_id = ? ORDER BY id", Long.class,
                    customer.getId());
            boolean demo = customer.getEmail().equals("aarav@zovira.test");
            int count = demo ? 6 : 3 + random.nextInt(3);
            for (int k = 0; k < count; k++) {
                int ageDays = demo ? new int[] {2, 1, 5, 20, 45, 70}[k] : 2 + random.nextInt(88);
                OrderStatus target = demo ? new OrderStatus[] {OrderStatus.PROCESSING, OrderStatus.CONFIRMED,
                        OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.CANCELLED}[k]
                        : ageDays > 12 ? (random.nextInt(10) == 0 ? OrderStatus.CANCELLED : OrderStatus.DELIVERED)
                        : ageDays > 5 ? OrderStatus.OUT_FOR_DELIVERY : ageDays > 3 ? OrderStatus.SHIPPED : OrderStatus.PROCESSING;
                if (demo && k == 3) {
                    ageDays = 4; // delivered recently, still inside its return window
                }
                List<Product> picks = new ArrayList<>();
                if (!reviewed.isEmpty() && target == OrderStatus.DELIVERED) {
                    Long id = reviewed.get(k % reviewed.size());
                    catalog.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(picks::add);
                }
                int extra = 1 + random.nextInt(2);
                while (picks.size() < extra || picks.isEmpty()) {
                    Product p = catalog.get(random.nextInt(catalog.size()));
                    if (!picks.contains(p) && p.getMinPrice().compareTo(new BigDecimal("80000")) < 0) {
                        picks.add(p);
                    }
                }
                Instant placedAt = now.minus(Duration.ofDays(ageDays)).minus(Duration.ofMinutes(random.nextInt(600)));
                Order order = build(customer, address, picks, placedAt, random, n++);
                progress(order, target, placedAt, random);
                backdates.add(new Object[] {order.getId(), Timestamp.from(placedAt)});
                if (demo && k == 4) {
                    OrderItem item = order.getItems().getFirst();
                    ReturnRequest ret = new ReturnRequest(SecureTokens.reference("RT", 10), item, 1, ReturnReason.DEFECTIVE,
                            "Stopped working after a few days.", item.netAmount().divide(BigDecimal.valueOf(item.getQuantity()), 2,
                                    RoundingMode.HALF_UP));
                    item.setStatus(OrderItemStatus.RETURN_REQUESTED);
                    returns.save(ret);
                }
            }
        }
        orders.flush();
        backdateTimelines(backdates);
        jdbc.update("""
                UPDATE reviews r SET verified_purchase = true
                 WHERE EXISTS (SELECT 1 FROM order_items oi JOIN orders o ON o.id = oi.order_id
                                WHERE o.user_id = r.user_id AND oi.product_id = r.product_id AND o.status = 'DELIVERED')
                """);
    }

    private Order build(User customer, Address address, List<Product> picks, Instant placedAt, Random random, int seq) {
        boolean cod = random.nextInt(3) == 0;
        PaymentMethod method = cod ? PaymentMethod.COD
                : new PaymentMethod[] {PaymentMethod.UPI, PaymentMethod.CARD, PaymentMethod.NETBANKING, PaymentMethod.WALLET}[random.nextInt(4)];
        String number = "ZV" + DateTimeFormatter.ofPattern("yyMMdd").format(placedAt.atZone(IST))
                + SecureTokens.reference("", 6).substring(1);
        Order order = new Order(number, customer, method, DeliveryOption.STANDARD, ShippingAddress.from(address), placedAt);
        order.setEstimatedDeliveryDate(placedAt.atZone(IST).toLocalDate().plusDays(4));
        Map<Long, Shipment> bySeller = new LinkedHashMap<>();
        BigDecimal mrp = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        int units = 0;
        for (Product p : picks) {
            ProductVariant v = p.defaultVariant();
            int qty = p.getMinPrice().compareTo(new BigDecimal("2000")) < 0 ? 1 + random.nextInt(2) : 1;
            OrderItem item = new OrderItem(p, v, p.getSeller(), p.primaryImageUrl(), qty, p.getCategory().getTaxRate(), placedAt);
            order.addItem(item);
            Shipment shipment = bySeller.computeIfAbsent(p.getSeller().getId(), id -> {
                Shipment s = new Shipment(order, p.getSeller(), SecureTokens.reference("SH", 10), order.getEstimatedDeliveryDate());
                order.addShipment(s);
                return s;
            });
            shipment.assignItem(item);
            mrp = mrp.add(v.getMrp().multiply(BigDecimal.valueOf(qty)));
            subtotal = subtotal.add(item.getLineTotal());
            BigDecimal rate = p.getCategory().getTaxRate();
            tax = tax.add(item.getLineTotal().multiply(rate).divide(rate.add(BigDecimal.valueOf(100)), 2, RoundingMode.HALF_UP));
            units += qty;
        }
        BigDecimal shipping = subtotal.compareTo(new BigDecimal("499")) >= 0 ? BigDecimal.ZERO : new BigDecimal("40");
        order.applyTotals(units, mrp, subtotal, null, BigDecimal.ZERO, shipping, BigDecimal.ZERO, tax, subtotal.add(shipping));
        order.start(OrderStatus.PLACED, cod ? "Cash on delivery" : null, placedAt);
        orders.save(order);
        Payment payment = new Payment(order, customer.getId(), cod ? PaymentProvider.COD : PaymentProvider.SANDBOX, method,
                order.getTotalAmount());
        if (!cod) {
            payment.setProviderOrderId("sbx_order_seed_" + seq);
            payment.markCaptured("sbx_pay_seed_" + seq, placedAt.plusSeconds(60));
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        payments.save(payment);
        order.transitionTo(OrderStatus.CONFIRMED, cod ? "Pay on delivery" : "Payment received via " + method, null, placedAt);
        return order;
    }

    private void progress(Order order, OrderStatus target, Instant placedAt, Random random) {
        if (target == OrderStatus.CANCELLED) {
            order.transitionTo(OrderStatus.CANCELLED, "Cancelled by customer: Ordered by mistake", order.getUser().getId(), placedAt);
            order.getItems().forEach(i -> i.setStatus(OrderItemStatus.CANCELLED));
            order.getShipments().forEach(s -> s.advance(ShipmentStatus.CANCELLED, null, "Order cancelled", placedAt));
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.recordRefund(order.getTotalAmount());
            }
            return;
        }
        List<ShipmentStatus> path = new ArrayList<>();
        path.add(ShipmentStatus.PROCESSING);
        if (target.ordinal() >= OrderStatus.SHIPPED.ordinal()) {
            path.add(ShipmentStatus.SHIPPED);
        }
        if (target.ordinal() >= OrderStatus.OUT_FOR_DELIVERY.ordinal()) {
            path.add(ShipmentStatus.OUT_FOR_DELIVERY);
        }
        if (target == OrderStatus.DELIVERED) {
            path.add(ShipmentStatus.DELIVERED);
        }
        if (target == OrderStatus.CONFIRMED) {
            path.clear();
        }
        for (Shipment s : order.getShipments()) {
            for (ShipmentStatus step : path) {
                boolean ship = step == ShipmentStatus.SHIPPED;
                shipments.advance(s, step, ship ? CARRIERS[random.nextInt(CARRIERS.length)] : null,
                        ship ? "AWB" + (100000000 + random.nextInt(899999999)) : null,
                        step == ShipmentStatus.SHIPPED ? s.getSeller().getPickupCity()
                                : step == ShipmentStatus.PROCESSING ? null : order.getShippingAddress().getCity(),
                        null, s.getSeller().getUser().getId());
            }
        }
    }

    /** Spreads each order's status history, shipment events and timestamps realistically after placement. */
    private void backdateTimelines(List<Object[]> orders) {
        for (Object[] row : orders) {
            Long orderId = (Long) row[0];
            Timestamp placed = (Timestamp) row[1];
            jdbc.update("""
                    UPDATE order_status_history h SET created_at = ?::timestamptz + (sub.rn - 1) * interval '19 hours'
                      FROM (SELECT id, row_number() OVER (ORDER BY id) rn FROM order_status_history WHERE order_id = ?) sub
                     WHERE h.id = sub.id
                    """, placed, orderId);
            jdbc.update("""
                    UPDATE shipment_events e SET occurred_at = ?::timestamptz + interval '6 hours' + (sub.rn - 1) * interval '19 hours'
                      FROM (SELECT e2.id, row_number() OVER (PARTITION BY e2.shipment_id ORDER BY e2.id) rn
                              FROM shipment_events e2 JOIN shipments s ON s.id = e2.shipment_id WHERE s.order_id = ?) sub
                     WHERE e.id = sub.id
                    """, placed, orderId);
            jdbc.update("UPDATE orders SET created_at = ?, confirmed_at = ?::timestamptz + interval '2 minutes' WHERE id = ?",
                    placed, placed, orderId);
            jdbc.update("""
                    UPDATE orders o SET delivered_at = (SELECT max(created_at) FROM order_status_history
                                                          WHERE order_id = o.id AND status = 'DELIVERED'),
                                        cancelled_at = (SELECT max(created_at) FROM order_status_history
                                                          WHERE order_id = o.id AND status = 'CANCELLED')
                     WHERE o.id = ?
                    """, orderId);
            jdbc.update("""
                    UPDATE shipments s SET
                      shipped_at = (SELECT min(occurred_at) FROM shipment_events WHERE shipment_id = s.id AND status = 'SHIPPED'),
                      delivered_at = (SELECT min(occurred_at) FROM shipment_events WHERE shipment_id = s.id AND status = 'DELIVERED'),
                      created_at = ?
                     WHERE s.order_id = ?
                    """, placed, orderId);
            jdbc.update("UPDATE order_items SET created_at = ? WHERE order_id = ?", placed, orderId);
            jdbc.update("UPDATE payments SET created_at = ?, paid_at = CASE WHEN paid_at IS NULL THEN NULL ELSE ?::timestamptz + interval '1 minute' END WHERE order_id = ?",
                    placed, placed, orderId);
        }
    }
}

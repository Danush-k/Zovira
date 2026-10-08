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
import com.zovira.order.entity.OrderStatusHistory;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import com.zovira.payment.repository.PaymentRepository;
import com.zovira.returns.entity.ReturnReason;
import com.zovira.returns.entity.ReturnRequest;
import com.zovira.returns.repository.ReturnRequestRepository;
import com.zovira.review.entity.Review;
import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentEvent;
import com.zovira.shipping.entity.ShipmentStatus;
import com.zovira.shipping.service.ShipmentService;
import com.zovira.user.entity.Address;
import com.zovira.user.entity.User;
import com.zovira.user.repository.AddressRepository;
import com.zovira.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    OrderSeedStep(OrderRepository orders, UserRepository users, AddressRepository addresses, ProductRepository products,
            PaymentRepository payments, ReturnRequestRepository returns, ShipmentService shipments,
            MongoTemplate mongoTemplate, Clock clock) {
        this.orders = orders;
        this.users = users;
        this.addresses = addresses;
        this.products = products;
        this.payments = payments;
        this.returns = returns;
        this.shipments = shipments;
        this.mongoTemplate = mongoTemplate;
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
            List<Long> reviewed = mongoTemplate.find(
                    Query.query(Criteria.where("userId").is(customer.getId())), Review.class).stream()
                    .map(r -> r.getProduct() != null ? r.getProduct().getId() : r.getProductId())
                    .filter(Objects::nonNull).toList();
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
                backdates.add(new Object[] {order.getId(), placedAt});
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
        backdateTimelines(backdates);
        List<Order> deliveredOrders = mongoTemplate.find(
                Query.query(Criteria.where("status").is(OrderStatus.DELIVERED)), Order.class);
        for (Order ord : deliveredOrders) {
            if (ord.getItems() != null) {
                for (OrderItem it : ord.getItems()) {
                    Long pId = it.getProduct() != null ? it.getProduct().getId() : it.getProductId();
                    if (pId != null) {
                        mongoTemplate.updateMulti(
                                Query.query(Criteria.where("userId").is(ord.getUserId()).and("productId").is(pId)),
                                new Update().set("verifiedPurchase", true),
                                Review.class);
                    }
                }
            }
        }
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
    private void backdateTimelines(List<Object[]> backdates) {
        for (Object[] row : backdates) {
            Long orderId = (Long) row[0];
            Instant placed = (Instant) row[1];
            Order order = orders.findById(orderId).orElse(null);
            if (order == null) continue;

            order.setCreatedAt(placed);
            order.setConfirmedAt(placed.plus(Duration.ofMinutes(2)));

            if (order.getStatusHistory() != null) {
                int i = 0;
                for (OrderStatusHistory h : order.getStatusHistory()) {
                    Instant histTime = placed.plus(Duration.ofHours(19L * i));
                    h.setCreatedAt(histTime);
                    if (h.getStatus() == OrderStatus.DELIVERED) {
                        order.setDeliveredAt(histTime);
                    } else if (h.getStatus() == OrderStatus.CANCELLED) {
                        order.setCancelledAt(histTime);
                    }
                    i++;
                }
            }

            if (order.getItems() != null) {
                for (OrderItem it : order.getItems()) {
                    it.setCreatedAt(placed);
                }
            }
            orders.save(order);

            List<Payment> orderPayments = payments.findByOrderIdOrderByIdDesc(orderId);
            for (Payment p : orderPayments) {
                p.setCreatedAt(placed);
                if (p.getPaidAt() != null) {
                    p.setPaidAt(placed.plus(Duration.ofMinutes(1)));
                }
                payments.save(p);
            }

            List<Shipment> orderShipments = mongoTemplate.find(
                    Query.query(Criteria.where("orderId").is(orderId)), Shipment.class);
            for (Shipment s : orderShipments) {
                s.setCreatedAt(placed);
                if (s.getEvents() != null) {
                    int k = 0;
                    for (ShipmentEvent e : s.getEvents()) {
                        Instant occurred = placed.plus(Duration.ofHours(6 + 19L * k));
                        e.setOccurredAt(occurred);
                        if (e.getStatus() == ShipmentStatus.SHIPPED && s.getShippedAt() == null) {
                            s.setShippedAt(occurred);
                        } else if (e.getStatus() == ShipmentStatus.DELIVERED) {
                            s.setDeliveredAt(occurred);
                        }
                        k++;
                    }
                }
                mongoTemplate.save(s);
            }
        }
    }
}

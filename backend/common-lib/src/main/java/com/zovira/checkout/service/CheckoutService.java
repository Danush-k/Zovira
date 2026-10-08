package com.zovira.checkout.service;

import com.zovira.cart.entity.Cart;
import com.zovira.cart.service.CartPricer;
import com.zovira.cart.service.CartPricer.PricedCart;
import com.zovira.cart.service.CartPricer.PricedLine;
import com.zovira.cart.service.CartService;
import com.zovira.checkout.dto.CheckoutPreview;
import com.zovira.checkout.dto.CheckoutRequest;
import com.zovira.checkout.dto.PlaceOrderResponse;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.util.SecureTokens;
import com.zovira.coupon.service.CouponLedger;
import com.zovira.delivery.dto.DeliveryEstimate;
import com.zovira.delivery.service.DeliveryService;
import com.zovira.inventory.service.InventoryService;
import com.zovira.order.entity.DeliveryOption;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.entity.PaymentMethod;
import com.zovira.order.entity.ShippingAddress;
import com.zovira.order.event.OrderEvent;
import com.zovira.order.repository.OrderRepository;
import com.zovira.payment.dto.PaymentIntent;
import com.zovira.payment.entity.Payment;
import com.zovira.payment.entity.PaymentProvider;
import com.zovira.payment.repository.PaymentRepository;
import com.zovira.payment.service.PaymentService;
import com.zovira.seller.entity.Seller;
import com.zovira.settings.dto.PlatformSettings;
import com.zovira.settings.service.SettingsService;
import com.zovira.shipping.entity.Shipment;
import com.zovira.user.entity.Address;
import com.zovira.user.entity.User;
import com.zovira.user.repository.UserRepository;
import com.zovira.user.service.AddressService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a cart into an order. Everything runs in one transaction: prices and coupon are recomputed
 * on the server, stock is reserved atomically, and the cart is cleared only once the order exists.
 * A client-supplied idempotency key makes double submits return the same order.
 */
@Service
public class CheckoutService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final CartService cartService;
    private final CartPricer pricer;
    private final AddressService addressService;
    private final DeliveryService deliveryService;
    private final InventoryService inventory;
    private final CouponLedger couponLedger;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentService paymentService;
    private final UserRepository users;
    private final SettingsService settingsService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CheckoutService(CartService cartService, CartPricer pricer, AddressService addressService,
            DeliveryService deliveryService, InventoryService inventory, CouponLedger couponLedger,
            OrderRepository orders, PaymentRepository payments, PaymentService paymentService, UserRepository users,
            SettingsService settingsService, ApplicationEventPublisher events, Clock clock) {
        this.cartService = cartService;
        this.pricer = pricer;
        this.addressService = addressService;
        this.deliveryService = deliveryService;
        this.inventory = inventory;
        this.couponLedger = couponLedger;
        this.orders = orders;
        this.payments = payments;
        this.paymentService = paymentService;
        this.users = users;
        this.settingsService = settingsService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public CheckoutPreview preview(Long userId, CheckoutRequest request) {
        Cart cart = cartService.cartFor(userId);
        Address address = addressService.find(userId, request.addressId());
        PricedCart priced = cartService.price(cart, userId, cart.getCouponCode(), request.deliveryOption());
        DeliveryEstimate estimate = deliveryService.estimate(address.getPincode());
        PlatformSettings settings = settingsService.current();
        String codReason = codUnavailableReason(priced, estimate, settings);
        return new CheckoutPreview(pricer.toResponse(priced, settings.maxQuantityPerItem()),
                estimate.standard() == null ? null : estimate.standard().date(),
                estimate.express() == null ? null : estimate.express().date(),
                estimate.express() != null && estimate.express().available(), estimate.serviceable(),
                codReason == null, codReason, settings.codFee(), users.findById(userId).map(User::isEmailVerified).orElse(false));
    }

    @Transactional
    public PlaceOrderResponse place(Long userId, CheckoutRequest request) {
        if (request.paymentMethod() == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Choose a payment method");
        }
        if (request.idempotencyKey() != null) {
            var existing = orders.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
            if (existing.isPresent()) {
                return replay(existing.get());
            }
        }
        User user = users.findById(userId).orElseThrow();
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.EMAIL_NOT_VERIFIED,
                    "Verify your email address before placing an order.");
        }
        Address address = addressService.find(userId, request.addressId());
        Cart cart = cartService.cartFor(userId);
        PricedCart priced = cartService.price(cart, userId, cart.getCouponCode(), request.deliveryOption());
        if (priced.purchasable().isEmpty()) {
            throw new BusinessException(ErrorCodes.CART_EMPTY, "Your cart is empty.");
        }
        if (priced.hasIssues()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCodes.CART_HAS_ISSUES,
                    "Some items in your cart changed. Review your cart before placing the order.");
        }
        if (cart.getCouponCode() != null && (priced.couponResult() == null || !priced.couponResult().valid())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCodes.COUPON_INVALID,
                    priced.couponResult() == null ? "Your coupon is no longer valid." : priced.couponResult().message());
        }

        LocalDate eta = deliveryService.estimatedDate(address.getPincode(), request.deliveryOption());
        PlatformSettings settings = settingsService.current();
        boolean cod = request.paymentMethod() == PaymentMethod.COD;
        BigDecimal codFee = BigDecimal.ZERO;
        if (cod) {
            String reason = codUnavailableReason(priced, deliveryService.estimate(address.getPincode()), settings);
            if (reason != null) {
                throw new BusinessException(ErrorCodes.PAYMENT_FAILED, reason);
            }
            codFee = settings.codFee();
        }

        Instant now = clock.instant();
        Order order = new Order(orderNumber(now), user, request.paymentMethod(), request.deliveryOption(),
                ShippingAddress.from(address), now);
        order.setIdempotencyKey(request.idempotencyKey());
        order.setEstimatedDeliveryDate(eta);

        Map<Long, Shipment> shipmentsBySeller = new LinkedHashMap<>();
        for (PricedLine line : priced.purchasable()) {
            OrderItem item = new OrderItem(line.product(), line.variant(), line.product().getSeller(),
                    line.product().primaryImageUrl(), line.quantity(), line.product().getCategory().getTaxRate(), now);
            item.setCouponDiscount(line.couponShare());
            order.addItem(item);
            Seller seller = line.product().getSeller();
            Shipment shipment = shipmentsBySeller.computeIfAbsent(seller.getId(), id -> {
                Shipment s = new Shipment(order, seller, SecureTokens.reference("SH", 10), eta);
                order.addShipment(s);
                return s;
            });
            shipment.assignItem(item);
        }
        order.applyTotals(priced.purchasable().stream().mapToInt(PricedLine::quantity).sum(), priced.mrpTotal(),
                priced.subtotal(), priced.couponResult() != null && priced.couponResult().valid() ? priced.couponCode() : null,
                priced.couponDiscount(), priced.shippingFee(), codFee, priced.taxIncluded(), priced.total().add(codFee));

        order.start(cod ? OrderStatus.PLACED : OrderStatus.PENDING_PAYMENT,
                cod ? "Cash on delivery" : "Waiting for payment", now);
        orders.save(order);
        inventory.reserve(order.getItems());
        if (priced.coupon() != null && priced.couponResult().valid()) {
            couponLedger.redeem(priced.coupon(), order);
        }
        cart.clearActiveItems();

        PaymentIntent intent = null;
        if (cod) {
            payments.save(new Payment(order, userId, PaymentProvider.COD, PaymentMethod.COD, order.getTotalAmount()));
            order.transitionTo(OrderStatus.CONFIRMED, "Pay " + "₹" + order.getTotalAmount().toPlainString()
                    + " on delivery", null, now);
            inventory.commit(order.getItems());
        } else {
            intent = paymentService.initiate(order, request.paymentMethod());
        }
        events.publishEvent(new OrderEvent(order.getId(), userId, order.getOrderNumber(), OrderEvent.Type.PLACED, null));
        return new PlaceOrderResponse(order.getOrderNumber(), order.getStatus().name(), !cod, intent);
    }

    private PlaceOrderResponse replay(Order order) {
        boolean awaiting = order.getStatus() == OrderStatus.PENDING_PAYMENT;
        PaymentIntent intent = awaiting ? paymentService.initiate(order, order.getPaymentMethod()) : null;
        return new PlaceOrderResponse(order.getOrderNumber(), order.getStatus().name(), awaiting, intent);
    }

    private static String codUnavailableReason(PricedCart cart, DeliveryEstimate estimate, PlatformSettings settings) {
        if (!settings.codEnabled()) {
            return "Cash on delivery is currently unavailable.";
        }
        if (!estimate.codAvailable()) {
            return "Cash on delivery isn't available for this PIN code.";
        }
        if (cart.total().compareTo(settings.codMaxOrder()) > 0) {
            return "Cash on delivery is available on orders up to ₹" + settings.codMaxOrder().toPlainString() + ".";
        }
        if (cart.purchasable().stream().anyMatch(l -> !l.product().isCodAvailable())) {
            return "Some items in your cart can't be paid for on delivery.";
        }
        return null;
    }

    private static String orderNumber(Instant now) {
        return "ZV" + NUMBER_DATE.format(now.atZone(DeliveryService.IST)) + SecureTokens.reference("", 6).substring(1);
    }
}

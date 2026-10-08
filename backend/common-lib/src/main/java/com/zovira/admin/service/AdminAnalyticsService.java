package com.zovira.admin.service;

import com.zovira.admin.dto.AdminOverview;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.returns.entity.ReturnStatus;
import com.zovira.returns.repository.ReturnRequestRepository;
import com.zovira.review.entity.ReviewStatus;
import com.zovira.review.repository.ReviewRepository;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.shipping.entity.ShipmentStatus;
import com.zovira.shipping.repository.ShipmentRepository;
import com.zovira.user.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Platform-wide reporting for the admin console on MongoDB.
 */
@Service
public class AdminAnalyticsService {

    private static final Set<OrderStatus> UNSETTLED = Set.of(
            OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.PENDING_PAYMENT);

    private final MongoTemplate mongoTemplate;
    private final SellerRepository sellers;
    private final ReviewRepository reviews;
    private final ReturnRequestRepository returns;
    private final ShipmentRepository shipments;

    public AdminAnalyticsService(MongoTemplate mongoTemplate, SellerRepository sellers, ReviewRepository reviews,
            ReturnRequestRepository returns, ShipmentRepository shipments) {
        this.mongoTemplate = mongoTemplate;
        this.sellers = sellers;
        this.reviews = reviews;
        this.returns = returns;
        this.shipments = shipments;
    }

    public AdminOverview overview() {
        Instant now = Instant.now();
        Instant d30 = now.minus(Duration.ofDays(30));
        Instant d60 = now.minus(Duration.ofDays(60));
        ZoneId zone = ZoneId.of("Asia/Kolkata");

        List<Order> allOrders = mongoTemplate.findAll(Order.class);
        List<Order> settledOrders = allOrders.stream()
                .filter(o -> o.getStatus() != null && !UNSETTLED.contains(o.getStatus()))
                .toList();

        BigDecimal r30 = BigDecimal.ZERO;
        BigDecimal r60 = BigDecimal.ZERO;
        long o30 = 0;
        long o60 = 0;
        BigDecimal lifetime = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;

        for (Order o : settledOrders) {
            BigDecimal amt = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            lifetime = lifetime.add(amt);
            Instant placed = o.getPlacedAt();
            if (placed != null) {
                if (!placed.isBefore(d30)) {
                    r30 = r30.add(amt);
                    o30++;
                    if (o.getRefundedAmount() != null) {
                        refunds = refunds.add(o.getRefundedAmount());
                    }
                } else if (!placed.isBefore(d60)) {
                    r60 = r60.add(amt);
                    o60++;
                }
            }
        }

        List<User> allUsers = mongoTemplate.findAll(User.class);
        long totalUsers = allUsers.size();
        long new30 = allUsers.stream().filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(d30)).count();
        long new60 = allUsers.stream().filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(d60) && u.getCreatedAt().isBefore(d30)).count();

        long activeProducts = mongoTemplate.count(Query.query(Criteria.where("status").is(ProductStatus.ACTIVE)), Product.class);
        long approvedSellers = sellers.countByStatus(SellerStatus.APPROVED);
        long buyers = allOrders.stream().map(Order::getUserId).filter(Objects::nonNull).distinct().count();

        // 30-day series
        LocalDate today = LocalDate.now(zone);
        List<AdminOverview.Point> revenueSeries = new ArrayList<>();
        List<AdminOverview.Point> signupSeries = new ArrayList<>();

        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal dayVal = BigDecimal.ZERO;
            long dayCount = 0;
            for (Order o : settledOrders) {
                if (o.getPlacedAt() != null && o.getPlacedAt().atZone(zone).toLocalDate().equals(day)) {
                    dayVal = dayVal.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
                    dayCount++;
                }
            }
            revenueSeries.add(new AdminOverview.Point(day, dayVal, dayCount));

            long userCount = 0;
            for (User u : allUsers) {
                if (u.getCreatedAt() != null && u.getCreatedAt().atZone(zone).toLocalDate().equals(day)) {
                    userCount++;
                }
            }
            signupSeries.add(new AdminOverview.Point(day, BigDecimal.ZERO, userCount));
        }

        // Category share (last 30 days)
        Map<String, CategoryAccumulator> catMap = new LinkedHashMap<>();
        // Top sellers (last 30 days)
        Map<Long, SellerAccumulator> sellerMap = new LinkedHashMap<>();

        for (Order o : settledOrders) {
            if (o.getPlacedAt() != null && !o.getPlacedAt().isBefore(d30)) {
                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        if (item.getStatus() != OrderItemStatus.CANCELLED) {
                            BigDecimal lineRev = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                            if (item.getCouponDiscount() != null) {
                                lineRev = lineRev.subtract(item.getCouponDiscount());
                            }
                            int qty = item.getQuantity();

                            // Category
                            Product p = item.getProduct();
                            if (p != null && p.getCategory() != null) {
                                Category c = p.getCategory();
                                Category target = c.getParent() != null ? c.getParent() : c;
                                catMap.computeIfAbsent(target.getSlug(), k -> new CategoryAccumulator(target.getName(), target.getSlug()))
                                        .add(lineRev, qty);
                            }

                            // Seller
                            Long sId = item.getSeller() != null ? item.getSeller().getId() : item.getSellerId();
                            if (sId != null) {
                                sellerMap.computeIfAbsent(sId, k -> new SellerAccumulator(sId))
                                        .add(lineRev, o.getId());
                            }
                        }
                    }
                }
            }
        }

        List<AdminOverview.CategoryShare> categories = catMap.values().stream()
                .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                .limit(8)
                .map(a -> new AdminOverview.CategoryShare(a.name, a.slug, a.revenue, a.units))
                .toList();

        List<AdminOverview.SellerRow> topSellers = sellerMap.values().stream()
                .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                .limit(5)
                .map(a -> {
                    Seller s = sellers.findById(a.sellerId).orElse(null);
                    String name = s != null ? s.getStoreName() : "Seller #" + a.sellerId;
                    String slug = s != null ? s.getSlug() : "";
                    BigDecimal rAvg = s != null ? s.getRatingAverage() : BigDecimal.ZERO;
                    int rCnt = s != null ? s.getRatingCount() : 0;
                    return new AdminOverview.SellerRow(a.sellerId, name, slug, a.revenue, a.orderIds.size(), rAvg, rCnt);
                })
                .toList();

        long pendingSellers = sellers.countByStatus(SellerStatus.PENDING);
        long pendingReviews = reviews.countByStatus(ReviewStatus.PENDING);
        long openReturns = returns.countByStatus(ReturnStatus.REQUESTED);
        long unshipped = shipments.countByStatusIn(EnumSet.of(ShipmentStatus.PENDING, ShipmentStatus.PROCESSING));

        List<AdminOverview.Alert> alerts = new ArrayList<>();
        if (pendingSellers > 0) {
            alerts.add(new AdminOverview.Alert("sellers", "seller applications waiting for review", "/admin/sellers?status=PENDING", pendingSellers));
        }
        if (pendingReviews > 0) {
            alerts.add(new AdminOverview.Alert("reviews", "reviews waiting for moderation", "/admin/reviews?status=PENDING", pendingReviews));
        }
        if (openReturns > 0) {
            alerts.add(new AdminOverview.Alert("returns", "return requests open", "/admin/returns", openReturns));
        }
        if (unshipped > 0) {
            alerts.add(new AdminOverview.Alert("orders", "shipments not yet dispatched", "/admin/orders", unshipped));
        }

        AdminOverview.Metrics metrics = new AdminOverview.Metrics(r30, r60, o30, o60,
                o30 == 0 ? BigDecimal.ZERO : r30.divide(BigDecimal.valueOf(o30), 2, RoundingMode.HALF_UP),
                new30, new60, totalUsers, approvedSellers,
                activeProducts, pendingSellers, pendingReviews, openReturns, unshipped,
                refunds, lifetime,
                totalUsers == 0 ? 0 : Math.round((double) buyers / totalUsers * 1000) / 10.0);

        return new AdminOverview(metrics, revenueSeries, signupSeries, categories, topSellers, alerts);
    }

    private static class CategoryAccumulator {
        final String name;
        final String slug;
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;

        CategoryAccumulator(String name, String slug) {
            this.name = name;
            this.slug = slug;
        }

        void add(BigDecimal rev, int qty) {
            this.revenue = this.revenue.add(rev);
            this.units += qty;
        }
    }

    private static class SellerAccumulator {
        final Long sellerId;
        BigDecimal revenue = BigDecimal.ZERO;
        final Set<Long> orderIds = new HashSet<>();

        SellerAccumulator(Long sellerId) {
            this.sellerId = sellerId;
        }

        void add(BigDecimal rev, Long orderId) {
            this.revenue = this.revenue.add(rev);
            if (orderId != null) this.orderIds.add(orderId);
        }
    }
}

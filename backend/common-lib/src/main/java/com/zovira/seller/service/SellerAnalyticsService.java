package com.zovira.seller.service;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.returns.entity.ReturnStatus;
import com.zovira.returns.repository.ReturnRequestRepository;
import com.zovira.seller.dto.SellerDashboard;
import com.zovira.seller.entity.Seller;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Seller reporting on MongoDB.
 */
@Service
public class SellerAnalyticsService {

    private static final Set<OrderStatus> UNSETTLED = Set.of(
            OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.PENDING_PAYMENT);

    private final MongoTemplate mongoTemplate;
    private final SellerOrderService orderService;
    private final ReturnRequestRepository returns;

    public SellerAnalyticsService(MongoTemplate mongoTemplate, SellerOrderService orderService,
            ReturnRequestRepository returns) {
        this.mongoTemplate = mongoTemplate;
        this.orderService = orderService;
        this.returns = returns;
    }

    public SellerDashboard dashboard(Seller seller, SellerCatalogService catalog) {
        Long sellerId = seller.getId();
        Instant now = Instant.now();
        Instant d30 = now.minus(Duration.ofDays(30));
        Instant d60 = now.minus(Duration.ofDays(60));
        ZoneId zone = ZoneId.of("Asia/Kolkata");

        List<Order> allOrders = mongoTemplate.findAll(Order.class);
        List<Order> settledOrders = allOrders.stream()
                .filter(o -> o.getStatus() != null && !UNSETTLED.contains(o.getStatus()))
                .toList();

        BigDecimal curRevenue = BigDecimal.ZERO;
        Set<Long> curOrderIds = new HashSet<>();
        long curUnits = 0;

        BigDecimal prevRevenue = BigDecimal.ZERO;
        Set<Long> prevOrderIds = new HashSet<>();

        BigDecimal lifetimeRevenue = BigDecimal.ZERO;

        Map<Long, ProductAccumulator> productMap = new LinkedHashMap<>();

        for (Order o : settledOrders) {
            Instant placed = o.getPlacedAt();
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    Long sId = item.getSeller() != null ? item.getSeller().getId() : item.getSellerId();
                    if (sellerId.equals(sId) && item.getStatus() != OrderItemStatus.CANCELLED) {
                        BigDecimal lineRev = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                        if (item.getCouponDiscount() != null) {
                            lineRev = lineRev.subtract(item.getCouponDiscount());
                        }
                        int qty = item.getQuantity();

                        lifetimeRevenue = lifetimeRevenue.add(lineRev);

                        if (placed != null) {
                            if (!placed.isBefore(d30)) {
                                curRevenue = curRevenue.add(lineRev);
                                curOrderIds.add(o.getId());
                                curUnits += qty;

                                Long pId = item.getProduct() != null ? item.getProduct().getId() : item.getProductId();
                                if (pId != null) {
                                    Product p = item.getProduct();
                                    String title = p != null ? p.getTitle() : "Product #" + pId;
                                    String slug = p != null ? p.getSlug() : "";
                                    String img = p != null ? p.primaryImageUrl() : item.getImageUrl();
                                    productMap.computeIfAbsent(pId, k -> new ProductAccumulator(pId, title, slug, img))
                                            .add(qty, lineRev);
                                }
                            } else if (!placed.isBefore(d60)) {
                                prevRevenue = prevRevenue.add(lineRev);
                                prevOrderIds.add(o.getId());
                            }
                        }
                    }
                }
            }
        }

        long activeProducts = mongoTemplate.count(
                Query.query(Criteria.where("sellerId").is(sellerId).and("status").is(ProductStatus.ACTIVE)),
                Product.class);
        long outOfStock = mongoTemplate.count(
                Query.query(Criteria.where("sellerId").is(sellerId).and("status").is(ProductStatus.ACTIVE).and("totalStock").lte(0)),
                Product.class);

        // 30-day series
        LocalDate today = LocalDate.now(zone);
        List<SellerDashboard.Point> series = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal dayRev = BigDecimal.ZERO;
            Set<Long> dayOrderIds = new HashSet<>();
            for (Order o : settledOrders) {
                if (o.getPlacedAt() != null && o.getPlacedAt().atZone(zone).toLocalDate().equals(day)) {
                    if (o.getItems() != null) {
                        for (OrderItem item : o.getItems()) {
                            Long sId = item.getSeller() != null ? item.getSeller().getId() : item.getSellerId();
                            if (sellerId.equals(sId) && item.getStatus() != OrderItemStatus.CANCELLED) {
                                BigDecimal lineRev = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                                if (item.getCouponDiscount() != null) {
                                    lineRev = lineRev.subtract(item.getCouponDiscount());
                                }
                                dayRev = dayRev.add(lineRev);
                                dayOrderIds.add(o.getId());
                            }
                        }
                    }
                }
            }
            series.add(new SellerDashboard.Point(day, dayRev, dayOrderIds.size()));
        }

        List<SellerDashboard.TopProduct> topProducts = productMap.values().stream()
                .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                .limit(5)
                .map(a -> new SellerDashboard.TopProduct(a.productId, a.title, a.slug, a.imageUrl, a.units, a.revenue))
                .toList();

        long curOrders = curOrderIds.size();
        SellerDashboard.Metrics metrics = new SellerDashboard.Metrics(
                curRevenue,
                prevRevenue,
                curOrders,
                prevOrderIds.size(),
                curUnits,
                curOrders == 0 ? BigDecimal.ZERO : curRevenue.divide(BigDecimal.valueOf(curOrders), 2, RoundingMode.HALF_UP),
                orderService.countPending(sellerId),
                returns.countBySellerIdAndStatus(sellerId, ReturnStatus.REQUESTED),
                activeProducts,
                outOfStock,
                lifetimeRevenue,
                seller.getRatingAverage(),
                seller.getRatingCount());

        return new SellerDashboard(
                metrics,
                series,
                topProducts,
                catalog.inventory(sellerId, true).stream().limit(8).toList(),
                orderService.list(sellerId, "open", 0, 5).content());
    }

    private static class ProductAccumulator {
        final Long productId;
        final String title;
        final String slug;
        final String imageUrl;
        long units = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAccumulator(Long productId, String title, String slug, String imageUrl) {
            this.productId = productId;
            this.title = title;
            this.slug = slug;
            this.imageUrl = imageUrl;
        }

        void add(long qty, BigDecimal rev) {
            this.units += qty;
            this.revenue = this.revenue.add(rev);
        }
    }
}

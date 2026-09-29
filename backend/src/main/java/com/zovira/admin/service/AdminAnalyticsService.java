package com.zovira.admin.service;

import com.zovira.admin.dto.AdminOverview;
import com.zovira.returns.entity.ReturnStatus;
import com.zovira.returns.repository.ReturnRequestRepository;
import com.zovira.review.entity.ReviewStatus;
import com.zovira.review.repository.ReviewRepository;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.shipping.entity.ShipmentStatus;
import com.zovira.shipping.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-wide reporting for the admin console. Revenue counts settled order value only, so
 * cancelled, failed and unpaid orders never inflate the numbers.
 */
@Service
public class AdminAnalyticsService {

    private static final String SETTLED = " o.status NOT IN ('CANCELLED', 'PAYMENT_FAILED', 'PENDING_PAYMENT') ";

    private final NamedParameterJdbcTemplate jdbc;
    private final SellerRepository sellers;
    private final ReviewRepository reviews;
    private final ReturnRequestRepository returns;
    private final ShipmentRepository shipments;

    public AdminAnalyticsService(NamedParameterJdbcTemplate jdbc, SellerRepository sellers, ReviewRepository reviews,
            ReturnRequestRepository returns, ShipmentRepository shipments) {
        this.jdbc = jdbc;
        this.sellers = sellers;
        this.reviews = reviews;
        this.returns = returns;
        this.shipments = shipments;
    }

    @Transactional(readOnly = true)
    public AdminOverview overview() {
        Map<String, Object> revenue = jdbc.getJdbcOperations().queryForMap("""
                SELECT COALESCE(SUM(total_amount) FILTER (WHERE placed_at >= now() - interval '30 days'), 0) AS r30,
                       COALESCE(SUM(total_amount) FILTER (WHERE placed_at >= now() - interval '60 days'
                                                            AND placed_at < now() - interval '30 days'), 0) AS r60,
                       COUNT(*) FILTER (WHERE placed_at >= now() - interval '30 days') AS o30,
                       COUNT(*) FILTER (WHERE placed_at >= now() - interval '60 days'
                                          AND placed_at < now() - interval '30 days') AS o60,
                       COALESCE(SUM(total_amount), 0) AS lifetime,
                       COALESCE(SUM(refunded_amount) FILTER (WHERE placed_at >= now() - interval '30 days'), 0) AS refunds
                  FROM orders o WHERE """ + SETTLED);
        Map<String, Object> users = jdbc.getJdbcOperations().queryForMap("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE created_at >= now() - interval '30 days') AS new30,
                       COUNT(*) FILTER (WHERE created_at >= now() - interval '60 days'
                                          AND created_at < now() - interval '30 days') AS new60
                  FROM users
                """);
        Map<String, Object> catalog = jdbc.getJdbcOperations().queryForMap("""
                SELECT (SELECT COUNT(*) FROM products WHERE status = 'ACTIVE') AS active_products,
                       (SELECT COUNT(*) FROM sellers WHERE status = 'APPROVED') AS sellers,
                       (SELECT COUNT(DISTINCT user_id) FROM orders) AS buyers
                """);

        List<AdminOverview.Point> revenueSeries = jdbc.getJdbcOperations().query("""
                SELECT d::date AS day,
                       COALESCE((SELECT SUM(total_amount) FROM orders o
                                  WHERE o.placed_at::date = d::date AND """ + SETTLED + """
                                ), 0) AS value,
                       COALESCE((SELECT COUNT(*) FROM orders o
                                  WHERE o.placed_at::date = d::date AND """ + SETTLED + """
                                ), 0) AS count
                  FROM generate_series(now()::date - interval '29 days', now()::date, interval '1 day') d
                 ORDER BY day
                """, (rs, i) -> new AdminOverview.Point(rs.getObject("day", Date.class).toLocalDate(),
                rs.getBigDecimal("value"), rs.getLong("count")));

        List<AdminOverview.Point> signups = jdbc.getJdbcOperations().query("""
                SELECT d::date AS day, 0::numeric AS value,
                       (SELECT COUNT(*) FROM users u WHERE u.created_at::date = d::date) AS count
                  FROM generate_series(now()::date - interval '29 days', now()::date, interval '1 day') d
                 ORDER BY day
                """, (rs, i) -> new AdminOverview.Point(rs.getObject("day", Date.class).toLocalDate(),
                rs.getBigDecimal("value"), rs.getLong("count")));

        List<AdminOverview.CategoryShare> categories = jdbc.getJdbcOperations().query("""
                SELECT COALESCE(parent.name, c.name) AS name, COALESCE(parent.slug, c.slug) AS slug,
                       SUM(oi.line_total - oi.coupon_discount) AS revenue, SUM(oi.quantity) AS units
                  FROM order_items oi
                  JOIN orders o ON o.id = oi.order_id
                  JOIN products p ON p.id = oi.product_id
                  JOIN categories c ON c.id = p.category_id
                  LEFT JOIN categories parent ON parent.id = c.parent_id
                 WHERE o.placed_at >= now() - interval '30 days' AND oi.status <> 'CANCELLED' AND """ + SETTLED + """
                 GROUP BY COALESCE(parent.name, c.name), COALESCE(parent.slug, c.slug)
                 ORDER BY revenue DESC LIMIT 8
                """, (rs, i) -> new AdminOverview.CategoryShare(rs.getString("name"), rs.getString("slug"),
                rs.getBigDecimal("revenue"), rs.getLong("units")));

        List<AdminOverview.SellerRow> topSellers = jdbc.getJdbcOperations().query("""
                SELECT s.id, s.store_name, s.slug, s.rating_average, s.rating_count,
                       SUM(oi.line_total - oi.coupon_discount) AS revenue, COUNT(DISTINCT oi.order_id) AS orders
                  FROM order_items oi
                  JOIN orders o ON o.id = oi.order_id
                  JOIN sellers s ON s.id = oi.seller_id
                 WHERE o.placed_at >= now() - interval '30 days' AND oi.status <> 'CANCELLED' AND """ + SETTLED + """
                 GROUP BY s.id, s.store_name, s.slug, s.rating_average, s.rating_count
                 ORDER BY revenue DESC LIMIT 5
                """, (rs, i) -> new AdminOverview.SellerRow(rs.getLong("id"), rs.getString("store_name"),
                rs.getString("slug"), rs.getBigDecimal("revenue"), rs.getLong("orders"),
                rs.getBigDecimal("rating_average"), rs.getInt("rating_count")));

        long pendingSellers = sellers.countByStatus(SellerStatus.PENDING);
        long pendingReviews = reviews.countByStatus(ReviewStatus.PENDING);
        long openReturns = returns.countByStatus(ReturnStatus.REQUESTED);
        long unshipped = shipments.countByStatusIn(EnumSet.of(ShipmentStatus.PENDING, ShipmentStatus.PROCESSING));

        long orders30 = num(revenue, "o30");
        BigDecimal revenue30 = money(revenue, "r30");
        long totalUsers = num(users, "total");
        long buyers = num(catalog, "buyers");

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

        AdminOverview.Metrics metrics = new AdminOverview.Metrics(revenue30, money(revenue, "r60"), orders30,
                num(revenue, "o60"),
                orders30 == 0 ? BigDecimal.ZERO : revenue30.divide(BigDecimal.valueOf(orders30), 2, RoundingMode.HALF_UP),
                num(users, "new30"), num(users, "new60"), totalUsers, num(catalog, "sellers"),
                num(catalog, "active_products"), pendingSellers, pendingReviews, openReturns, unshipped,
                money(revenue, "refunds"), money(revenue, "lifetime"),
                totalUsers == 0 ? 0 : Math.round((double) buyers / totalUsers * 1000) / 10.0);

        return new AdminOverview(metrics, revenueSeries, signups, categories, topSellers, alerts);
    }

    private static long num(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal money(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}

package com.zovira.seller.service;

import com.zovira.returns.entity.ReturnStatus;
import com.zovira.returns.repository.ReturnRequestRepository;
import com.zovira.seller.dto.SellerDashboard;
import com.zovira.seller.entity.Seller;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seller reporting. Revenue counts only items the seller actually sold (net of their share of
 * coupons) on orders that were paid for and not cancelled.
 */
@Service
public class SellerAnalyticsService {

    /** Excludes cancelled, failed and unpaid orders so revenue reflects money actually earned. */
    private static final String EARNED =
            " o.status NOT IN ('CANCELLED', 'PAYMENT_FAILED', 'PENDING_PAYMENT') AND oi.status <> 'CANCELLED' ";

    private final NamedParameterJdbcTemplate jdbc;
    private final SellerOrderService orderService;
    private final ReturnRequestRepository returns;

    public SellerAnalyticsService(NamedParameterJdbcTemplate jdbc, SellerOrderService orderService,
            ReturnRequestRepository returns) {
        this.jdbc = jdbc;
        this.orderService = orderService;
        this.returns = returns;
    }

    @Transactional(readOnly = true)
    public SellerDashboard dashboard(Seller seller, SellerCatalogService catalog) {
        MapSqlParameterSource params = new MapSqlParameterSource("sellerId", seller.getId());
        Map<String, Object> current = jdbc.queryForMap("""
                SELECT COALESCE(SUM(oi.line_total - oi.coupon_discount), 0) AS revenue,
                       COUNT(DISTINCT oi.order_id) AS orders,
                       COALESCE(SUM(oi.quantity), 0) AS units
                  FROM order_items oi JOIN orders o ON o.id = oi.order_id
                 WHERE oi.seller_id = :sellerId AND o.placed_at >= now() - interval '30 days' AND """ + EARNED, params);
        Map<String, Object> previous = jdbc.queryForMap("""
                SELECT COALESCE(SUM(oi.line_total - oi.coupon_discount), 0) AS revenue,
                       COUNT(DISTINCT oi.order_id) AS orders
                  FROM order_items oi JOIN orders o ON o.id = oi.order_id
                 WHERE oi.seller_id = :sellerId AND o.placed_at >= now() - interval '60 days'
                   AND o.placed_at < now() - interval '30 days' AND """ + EARNED, params);
        Map<String, Object> lifetime = jdbc.queryForMap("""
                SELECT COALESCE(SUM(oi.line_total - oi.coupon_discount), 0) AS revenue
                  FROM order_items oi JOIN orders o ON o.id = oi.order_id
                 WHERE oi.seller_id = :sellerId AND """ + EARNED, params);
        Map<String, Object> catalogCounts = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active,
                       COUNT(*) FILTER (WHERE status = 'ACTIVE' AND total_stock = 0) AS out_of_stock
                  FROM products WHERE seller_id = :sellerId
                """, params);

        List<SellerDashboard.Point> series = jdbc.query("""
                SELECT d::date AS day,
                       COALESCE((SELECT SUM(oi.line_total - oi.coupon_discount)
                                   FROM order_items oi JOIN orders o ON o.id = oi.order_id
                                  WHERE oi.seller_id = :sellerId AND o.placed_at::date = d::date AND """ + EARNED + """
                                ), 0) AS revenue,
                       COALESCE((SELECT COUNT(DISTINCT oi.order_id)
                                   FROM order_items oi JOIN orders o ON o.id = oi.order_id
                                  WHERE oi.seller_id = :sellerId AND o.placed_at::date = d::date AND """ + EARNED + """
                                ), 0) AS orders
                  FROM generate_series(now()::date - interval '29 days', now()::date, interval '1 day') d
                 ORDER BY day
                """, params, (rs, i) -> new SellerDashboard.Point(rs.getObject("day", Date.class).toLocalDate(),
                rs.getBigDecimal("revenue"), rs.getLong("orders")));

        List<SellerDashboard.TopProduct> top = jdbc.query("""
                SELECT p.id, p.title, p.slug, SUM(oi.quantity) AS units,
                       SUM(oi.line_total - oi.coupon_discount) AS revenue,
                       (SELECT url FROM product_images WHERE product_id = p.id ORDER BY position LIMIT 1) AS image_url
                  FROM order_items oi
                  JOIN orders o ON o.id = oi.order_id
                  JOIN products p ON p.id = oi.product_id
                 WHERE oi.seller_id = :sellerId AND o.placed_at >= now() - interval '30 days' AND """ + EARNED + """
                 GROUP BY p.id, p.title, p.slug
                 ORDER BY revenue DESC
                 LIMIT 5
                """, params, (rs, i) -> new SellerDashboard.TopProduct(rs.getLong("id"), rs.getString("title"),
                rs.getString("slug"), rs.getString("image_url"), rs.getLong("units"), rs.getBigDecimal("revenue")));

        BigDecimal revenue = (BigDecimal) current.get("revenue");
        long orders = ((Number) current.get("orders")).longValue();
        SellerDashboard.Metrics metrics = new SellerDashboard.Metrics(revenue, (BigDecimal) previous.get("revenue"),
                orders, ((Number) previous.get("orders")).longValue(), ((Number) current.get("units")).longValue(),
                orders == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orders), 2, java.math.RoundingMode.HALF_UP),
                orderService.countPending(seller.getId()),
                returns.countBySellerIdAndStatus(seller.getId(), ReturnStatus.REQUESTED),
                ((Number) catalogCounts.get("active")).longValue(),
                ((Number) catalogCounts.get("out_of_stock")).longValue(), (BigDecimal) lifetime.get("revenue"),
                seller.getRatingAverage(), seller.getRatingCount());

        return new SellerDashboard(metrics, series, top, catalog.inventory(seller.getId(), true).stream().limit(8).toList(),
                orderService.list(seller.getId(), "open", 0, 5).content());
    }
}

package com.zovira.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminOverview(Metrics metrics, List<Point> revenueSeries, List<Point> signupSeries,
        List<CategoryShare> categoryShare, List<SellerRow> topSellers, List<Alert> alerts) {

    public record Metrics(
            BigDecimal revenue30d,
            BigDecimal revenuePrevious30d,
            long orders30d,
            long ordersPrevious30d,
            BigDecimal averageOrderValue,
            long newUsers30d,
            long newUsersPrevious30d,
            long totalUsers,
            long totalSellers,
            long activeProducts,
            long pendingSellers,
            long pendingReviews,
            long openReturns,
            long unshippedOrders,
            BigDecimal refunds30d,
            BigDecimal lifetimeRevenue,
            double conversionRate) {
    }

    public record Point(LocalDate date, BigDecimal value, long count) {
    }

    public record CategoryShare(String name, String slug, BigDecimal revenue, long units) {
    }

    public record SellerRow(Long id, String storeName, String slug, BigDecimal revenue, long orders,
            BigDecimal ratingAverage, int ratingCount) {
    }

    public record Alert(String kind, String message, String link, long count) {
    }
}

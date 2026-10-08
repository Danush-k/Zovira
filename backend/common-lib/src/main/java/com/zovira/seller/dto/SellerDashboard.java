package com.zovira.seller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SellerDashboard(
        Metrics metrics,
        List<Point> revenueSeries,
        List<TopProduct> topProducts,
        List<InventoryRow> lowStock,
        List<SellerOrderRow> recentOrders) {

    public record Metrics(
            BigDecimal revenue30d,
            BigDecimal revenuePrevious30d,
            long orders30d,
            long ordersPrevious30d,
            long unitsSold30d,
            BigDecimal averageOrderValue,
            long pendingShipments,
            long openReturns,
            long activeProducts,
            long outOfStock,
            BigDecimal lifetimeRevenue,
            BigDecimal ratingAverage,
            long ratingCount) {
    }

    public record Point(LocalDate date, BigDecimal revenue, long orders) {
    }

    public record TopProduct(Long productId, String title, String slug, String imageUrl, long units, BigDecimal revenue) {
    }
}

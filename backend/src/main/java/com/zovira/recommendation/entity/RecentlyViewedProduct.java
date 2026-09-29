package com.zovira.recommendation.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "recently_viewed_products")
public class RecentlyViewedProduct extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false)
    private int viewCount = 1;

    @Column(nullable = false)
    private Instant viewedAt;

    protected RecentlyViewedProduct() {
    }

    public RecentlyViewedProduct(Long userId, Product product, Instant viewedAt) {
        this.userId = userId;
        this.product = product;
        this.viewedAt = viewedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Product getProduct() {
        return product;
    }

    public int getViewCount() {
        return viewCount;
    }

    public Instant getViewedAt() {
        return viewedAt;
    }
}

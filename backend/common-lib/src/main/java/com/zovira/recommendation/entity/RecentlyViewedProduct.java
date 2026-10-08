package com.zovira.recommendation.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.common.entity.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "recently_viewed_products")
public class RecentlyViewedProduct extends BaseEntity {

    @Indexed
    private Long userId;

    private Product product;

    @Indexed
    private Long productId;

    private int viewCount = 1;
    private Instant viewedAt = Instant.now();

    public RecentlyViewedProduct() {
    }

    public RecentlyViewedProduct(Long userId, Product product, Instant viewedAt) {
        this.userId = userId;
        this.product = product;
        this.productId = product != null ? product.getId() : null;
        this.viewedAt = viewedAt != null ? viewedAt : Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
    }

    public Long getProductId() {
        if (productId != null) return productId;
        return product != null ? product.getId() : null;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public Instant getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Instant viewedAt) {
        this.viewedAt = viewedAt;
    }
}

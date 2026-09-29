package com.zovira.wishlist.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false, updatable = false)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    /** Price when saved; lets the wishlist surface price drops. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAdd;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WishlistItem() {
    }

    public WishlistItem(Product product, ProductVariant variant, BigDecimal priceAtAdd, Instant createdAt) {
        this.product = product;
        this.variant = variant;
        this.priceAtAdd = priceAtAdd;
        this.createdAt = createdAt;
    }

    void setWishlist(Wishlist wishlist) {
        this.wishlist = wishlist;
    }

    public Wishlist getWishlist() {
        return wishlist;
    }

    public Product getProduct() {
        return product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public BigDecimal getPriceAtAdd() {
        return priceAtAdd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

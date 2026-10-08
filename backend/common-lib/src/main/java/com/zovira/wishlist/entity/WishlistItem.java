package com.zovira.wishlist.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.Instant;

public class WishlistItem extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Wishlist wishlist;
    private Long wishlistId;
    private Product product;
    private Long productId;
    private ProductVariant variant;
    private Long variantId;
    private BigDecimal priceAtAdd;
    private Instant createdAt = Instant.now();

    public WishlistItem() {
    }

    public WishlistItem(Product product, ProductVariant variant, BigDecimal priceAtAdd, Instant createdAt) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
        this.variant = variant;
        this.variantId = variant != null ? variant.getId() : null;
        this.priceAtAdd = priceAtAdd;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void setWishlist(Wishlist wishlist) {
        this.wishlist = wishlist;
        this.wishlistId = wishlist != null ? wishlist.getId() : null;
    }

    public Wishlist getWishlist() {
        return wishlist;
    }

    public Long getWishlistId() {
        if (wishlistId != null) return wishlistId;
        return wishlist != null ? wishlist.getId() : null;
    }

    public void setWishlistId(Long wishlistId) {
        this.wishlistId = wishlistId;
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

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
        this.variantId = variant != null ? variant.getId() : null;
    }

    public Long getVariantId() {
        if (variantId != null) return variantId;
        return variant != null ? variant.getId() : null;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getPriceAtAdd() {
        return priceAtAdd;
    }

    public void setPriceAtAdd(BigDecimal priceAtAdd) {
        this.priceAtAdd = priceAtAdd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

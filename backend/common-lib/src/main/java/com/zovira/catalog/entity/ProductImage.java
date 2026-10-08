package com.zovira.catalog.entity;

import com.zovira.common.entity.BaseEntity;
import java.time.Instant;

public class ProductImage extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Product product;
    private Long productId;
    @org.springframework.data.annotation.Transient
    private ProductVariant variant;
    private Long variantId;
    private String url;
    private String altText;
    private int position;
    private Instant createdAt = Instant.now();

    public ProductImage() {
    }

    public ProductImage(String url, String altText, int position) {
        this.url = url;
        this.altText = altText;
        this.position = position;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }

    public Product getProduct() {
        return product;
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
        if (variant != null) {
            this.variantId = variant.getId();
        }
    }

    public Long getVariantId() {
        if (variantId != null) return variantId;
        return variant != null ? variant.getId() : null;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

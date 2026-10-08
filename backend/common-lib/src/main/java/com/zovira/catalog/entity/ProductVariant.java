package com.zovira.catalog.entity;

import com.zovira.common.entity.AuditableEntity;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Document(collection = "product_variants")
public class ProductVariant extends AuditableEntity {

    @org.springframework.data.annotation.Transient
    private Product product;

    @Indexed
    private Long productId;

    @Indexed(unique = true)
    private String sku;

    private String name;
    private Map<String, String> options = new LinkedHashMap<>();
    private BigDecimal price;
    private BigDecimal mrp;
    private Integer weightGrams;
    private boolean defaultVariant;
    private boolean active = true;
    private int position;

    @Version
    private Long version;

    public ProductVariant() {
    }

    public ProductVariant(String sku, String name, BigDecimal price, BigDecimal mrp) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.mrp = mrp;
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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public Integer getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(Integer weightGrams) {
        this.weightGrams = weightGrams;
    }

    public boolean isDefaultVariant() {
        return defaultVariant;
    }

    public void setDefaultVariant(boolean defaultVariant) {
        this.defaultVariant = defaultVariant;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

package com.zovira.inventory.entity;

import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.BaseEntity;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "inventory")
public class Inventory extends BaseEntity {

    private ProductVariant variant;

    @Indexed(unique = true)
    private Long variantId;

    private int quantityAvailable;
    private int quantityReserved;
    private int lowStockThreshold = 5;
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    public Inventory() {
    }

    public Inventory(ProductVariant variant, int quantityAvailable) {
        this.variant = variant;
        this.variantId = variant != null ? variant.getId() : null;
        this.quantityAvailable = quantityAvailable;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public boolean isLowStock() {
        return quantityAvailable > 0 && quantityAvailable <= lowStockThreshold;
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

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
        touch();
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
        touch();
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

package com.zovira.inventory.entity;

import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, unique = true, updatable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private int quantityAvailable;

    @Column(nullable = false)
    private int quantityReserved;

    @Column(nullable = false)
    private int lowStockThreshold = 5;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Inventory() {
    }

    public Inventory(ProductVariant variant, int quantityAvailable) {
        this.variant = variant;
        this.quantityAvailable = quantityAvailable;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public boolean isLowStock() {
        return quantityAvailable > 0 && quantityAvailable <= lowStockThreshold;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getQuantityReserved() {
        return quantityReserved;
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
}

package com.zovira.cart.entity;

import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, updatable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean savedForLater;

    /** Unit price when the item was added; used to tell shoppers about price changes. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAdd;

    protected CartItem() {
    }

    public CartItem(ProductVariant variant, int quantity, BigDecimal priceAtAdd) {
        this.variant = variant;
        this.quantity = quantity;
        this.priceAtAdd = priceAtAdd;
    }

    void setCart(Cart cart) {
        this.cart = cart;
    }

    public Cart getCart() {
        return cart;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSavedForLater() {
        return savedForLater;
    }

    public void setSavedForLater(boolean savedForLater) {
        this.savedForLater = savedForLater;
    }

    public BigDecimal getPriceAtAdd() {
        return priceAtAdd;
    }

    public void setPriceAtAdd(BigDecimal priceAtAdd) {
        this.priceAtAdd = priceAtAdd;
    }
}

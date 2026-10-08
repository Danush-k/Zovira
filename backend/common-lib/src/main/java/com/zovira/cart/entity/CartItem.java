package com.zovira.cart.entity;

import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.AuditableEntity;
import java.math.BigDecimal;

public class CartItem extends AuditableEntity {

    @org.springframework.data.annotation.Transient
    private Cart cart;
    private Long cartId;
    private ProductVariant variant;
    private Long variantId;
    private int quantity;
    private boolean savedForLater;
    private BigDecimal priceAtAdd;

    public CartItem() {
    }

    public CartItem(ProductVariant variant, int quantity, BigDecimal priceAtAdd) {
        this.variant = variant;
        this.variantId = variant != null ? variant.getId() : null;
        this.quantity = quantity;
        this.priceAtAdd = priceAtAdd;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
        this.cartId = cart != null ? cart.getId() : null;
    }

    public Cart getCart() {
        return cart;
    }

    public Long getCartId() {
        if (cartId != null) return cartId;
        return cart != null ? cart.getId() : null;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
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

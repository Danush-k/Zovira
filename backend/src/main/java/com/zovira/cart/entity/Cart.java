package com.zovira.cart.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
public class Cart extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(length = 40)
    private String couponCode;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC, id DESC")
    private List<CartItem> items = new ArrayList<>();

    @Version
    private long version;

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
    }

    public Optional<CartItem> findItem(Long itemId) {
        return items.stream().filter(i -> i.getId().equals(itemId)).findFirst();
    }

    public Optional<CartItem> findByVariant(Long variantId) {
        return items.stream().filter(i -> i.getVariant().getId().equals(variantId)).findFirst();
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public List<CartItem> activeItems() {
        return items.stream().filter(i -> !i.isSavedForLater()).toList();
    }

    public void clearActiveItems() {
        items.removeIf(i -> !i.isSavedForLater());
        couponCode = null;
    }

    public User getUser() {
        return user;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public List<CartItem> getItems() {
        return items;
    }
}

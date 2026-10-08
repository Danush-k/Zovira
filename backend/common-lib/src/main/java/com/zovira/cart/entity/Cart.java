package com.zovira.cart.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Document(collection = "carts")
public class Cart extends AuditableEntity {

    private User user;

    @Indexed(unique = true)
    private Long userId;

    private String couponCode;
    private List<CartItem> items = new ArrayList<>();

    @Version
    private Long version;

    public Cart() {
    }

    public Cart(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }

    public Optional<CartItem> findItem(Long itemId) {
        return items.stream().filter(i -> itemId != null && itemId.equals(i.getId())).findFirst();
    }

    public Optional<CartItem> findByVariant(Long variantId) {
        return items.stream().filter(i -> i.getVariant() != null && variantId != null && variantId.equals(i.getVariant().getId())).findFirst();
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        if (item.getId() == null) {
            long maxId = items.stream().mapToLong(i -> i.getId() != null ? i.getId() : 0).max().orElse(0);
            item.setId(maxId + 1);
        }
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

    public void setUser(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }

    public Long getUserId() {
        if (userId != null) return userId;
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public void setItems(List<CartItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

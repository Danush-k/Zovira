package com.zovira.wishlist.entity;

import com.zovira.common.entity.BaseEntity;
import com.zovira.user.entity.User;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Document(collection = "wishlists")
public class Wishlist extends BaseEntity {

    private User user;

    @Indexed(unique = true)
    private Long userId;

    private Instant createdAt = Instant.now();
    private List<WishlistItem> items = new ArrayList<>();

    public Wishlist() {
    }

    public Wishlist(User user, Instant createdAt) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Optional<WishlistItem> findByProduct(Long productId) {
        return items.stream().filter(i -> i.getProduct() != null && productId != null && productId.equals(i.getProduct().getId())).findFirst();
    }

    public void addItem(WishlistItem item) {
        item.setWishlist(this);
        if (item.getId() == null) {
            long maxId = items.stream().mapToLong(i -> i.getId() != null ? i.getId() : 0).max().orElse(0);
            item.setId(maxId + 1);
        }
        items.add(item);
    }

    public void removeItem(WishlistItem item) {
        items.remove(item);
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<WishlistItem> getItems() {
        return items;
    }

    public void setItems(List<WishlistItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}

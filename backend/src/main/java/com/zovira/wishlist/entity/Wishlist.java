package com.zovira.wishlist.entity;

import com.zovira.common.entity.BaseEntity;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "wishlists")
public class Wishlist extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC, id DESC")
    private List<WishlistItem> items = new ArrayList<>();

    protected Wishlist() {
    }

    public Wishlist(User user, Instant createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

    public Optional<WishlistItem> findByProduct(Long productId) {
        return items.stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst();
    }

    public void addItem(WishlistItem item) {
        item.setWishlist(this);
        items.add(item);
    }

    public void removeItem(WishlistItem item) {
        items.remove(item);
    }

    public User getUser() {
        return user;
    }

    public List<WishlistItem> getItems() {
        return items;
    }
}

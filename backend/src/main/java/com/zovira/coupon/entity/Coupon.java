package com.zovira.coupon.entity;

import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "coupons")
public class Coupon extends AuditableEntity {

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(nullable = false)
    private Instant startsAt;

    private Instant expiresAt;

    private Integer usageLimit;

    @Column(nullable = false)
    private int perUserLimit = 1;

    @Column(nullable = false)
    private int usedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CouponScope scope = CouponScope.ALL;

    @Column(nullable = false)
    private boolean userSpecific;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "coupon_categories",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "coupon_products",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> products = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "coupon_users",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> users = new HashSet<>();

    @Version
    private long version;

    protected Coupon() {
    }

    public Coupon(String code, String description, DiscountType discountType, BigDecimal discountValue,
            Instant startsAt) {
        this.code = normalizeCode(code);
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startsAt = startsAt;
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isWithinWindow(Instant now) {
        return !now.isBefore(startsAt) && (expiresAt == null || now.isBefore(expiresAt));
    }

    public boolean hasRemainingUses() {
        return usageLimit == null || usedCount < usageLimit;
    }

    public void incrementUsage() {
        usedCount++;
    }

    public void decrementUsage() {
        if (usedCount > 0) {
            usedCount--;
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = normalizeCode(code);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(int perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public CouponScope getScope() {
        return scope;
    }

    public void setScope(CouponScope scope) {
        this.scope = scope;
    }

    public boolean isUserSpecific() {
        return userSpecific;
    }

    public void setUserSpecific(boolean userSpecific) {
        this.userSpecific = userSpecific;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public Set<User> getUsers() {
        return users;
    }
}

package com.zovira.coupon.entity;

import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Document(collection = "coupons")
public class Coupon extends AuditableEntity {

    @Indexed(unique = true)
    private String code;

    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    private BigDecimal maxDiscount;
    private Instant startsAt;
    private Instant expiresAt;
    private Integer usageLimit;
    private int perUserLimit = 1;
    private int usedCount;
    private CouponScope scope = CouponScope.ALL;
    private boolean userSpecific;
    private boolean active = true;

    private Set<Category> categories = new HashSet<>();
    private Set<Product> products = new HashSet<>();
    private Set<User> users = new HashSet<>();

    @Version
    private Long version;

    public Coupon() {
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

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
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

    public void setCategories(Set<Category> categories) {
        this.categories = categories == null ? new HashSet<>() : categories;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void setProducts(Set<Product> products) {
        this.products = products == null ? new HashSet<>() : products;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users == null ? new HashSet<>() : users;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

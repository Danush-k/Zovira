package com.zovira.seller.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "sellers")
public class Seller extends AuditableEntity {

    private User user;

    @Indexed(unique = true)
    private Long userId;

    private String storeName;

    @Indexed(unique = true)
    private String slug;

    private String description;
    private String logoUrl;
    private String gstin;
    private String supportEmail;
    private String supportPhone;
    private String pickupLine1;
    private String pickupCity;
    private String pickupState;
    private String pickupPincode;
    private SellerStatus status = SellerStatus.PENDING;
    private String statusReason;
    private BigDecimal ratingAverage = BigDecimal.ZERO;
    private int ratingCount;
    private Instant approvedAt;

    @Version
    private Long version;

    public Seller() {
    }

    public Seller(User user, String storeName, String slug) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.storeName = storeName;
        this.slug = slug;
    }

    public boolean isApproved() {
        return status == SellerStatus.APPROVED;
    }

    public void approve(Instant now) {
        this.status = SellerStatus.APPROVED;
        this.statusReason = null;
        this.approvedAt = now;
    }

    public void changeStatus(SellerStatus status, String reason) {
        this.status = status;
        this.statusReason = reason;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public Long getUserId() {
        if (userId != null) {
            return userId;
        }
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
    }

    public String getPickupLine1() {
        return pickupLine1;
    }

    public void setPickupLine1(String pickupLine1) {
        this.pickupLine1 = pickupLine1;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public void setPickupCity(String pickupCity) {
        this.pickupCity = pickupCity;
    }

    public String getPickupState() {
        return pickupState;
    }

    public void setPickupState(String pickupState) {
        this.pickupState = pickupState;
    }

    public String getPickupPincode() {
        return pickupPincode;
    }

    public void setPickupPincode(String pickupPincode) {
        this.pickupPincode = pickupPincode;
    }

    public SellerStatus getStatus() {
        return status;
    }

    public void setStatus(SellerStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(BigDecimal ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public void updateRating(BigDecimal average, int count) {
        this.ratingAverage = average;
        this.ratingCount = count;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

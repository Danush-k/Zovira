package com.zovira.seller.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sellers")
public class Seller extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String storeName;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(length = 512)
    private String logoUrl;

    @Column(length = 15)
    private String gstin;

    @Column(length = 254)
    private String supportEmail;

    @Column(length = 20)
    private String supportPhone;

    @Column(length = 200)
    private String pickupLine1;

    @Column(length = 80)
    private String pickupCity;

    @Column(length = 80)
    private String pickupState;

    @Column(length = 10)
    private String pickupPincode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerStatus status = SellerStatus.PENDING;

    @Column(length = 500)
    private String statusReason;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(nullable = false)
    private int ratingCount;

    private Instant approvedAt;

    @Version
    private long version;

    protected Seller() {
    }

    public Seller(User user, String storeName, String slug) {
        this.user = user;
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

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getSlug() {
        return slug;
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

    public String getStatusReason() {
        return statusReason;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void updateRating(BigDecimal average, int count) {
        this.ratingAverage = average;
        this.ratingCount = count;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }
}

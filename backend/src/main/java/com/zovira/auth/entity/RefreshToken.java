package com.zovira.auth.entity;

import com.zovira.common.entity.BaseEntity;
import com.zovira.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private UUID familyId;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @Column(length = 40)
    private String revokedReason;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastUsedAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt, String userAgent,
            String ipAddress, Instant createdAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now, String reason) {
        if (revokedAt == null) {
            revokedAt = now;
            revokedReason = reason;
        }
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}

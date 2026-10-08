package com.zovira.auth.entity;

import com.zovira.common.entity.BaseEntity;
import com.zovira.user.entity.User;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    private User user;

    @Indexed
    private Long userId;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private UUID familyId;

    @Indexed
    private Instant expiresAt;

    private Instant revokedAt;
    private String revokedReason;
    private String userAgent;
    private String ipAddress;
    private Instant createdAt;
    private Instant lastUsedAt;

    public RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt, String userAgent,
            String ipAddress, Instant createdAt) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String reason) {
        this.revokedReason = reason;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}

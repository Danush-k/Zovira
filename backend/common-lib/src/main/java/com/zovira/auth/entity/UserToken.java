package com.zovira.auth.entity;

import com.zovira.common.entity.BaseEntity;
import com.zovira.user.entity.User;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Single-use, time-boxed token for email verification or password reset. Only the hash is stored. */
@Document(collection = "user_tokens")
public class UserToken extends BaseEntity {

    private User user;

    @Indexed
    private Long userId;

    private UserTokenType type;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private Instant expiresAt;

    private Instant usedAt;
    private Instant createdAt;

    public UserToken() {
    }

    public UserToken(User user, UserTokenType type, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
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

    public UserTokenType getType() {
        return type;
    }

    public void setType(UserTokenType type) {
        this.type = type;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

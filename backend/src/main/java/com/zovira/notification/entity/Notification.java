package com.zovira.notification.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 255)
    private String link;

    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long userId, NotificationType type, String title, String message, String link,
            Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.createdAt = createdAt;
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

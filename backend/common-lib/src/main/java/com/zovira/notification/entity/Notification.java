package com.zovira.notification.entity;

import com.zovira.common.entity.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "notifications")
public class Notification extends BaseEntity {

    @Indexed
    private Long userId;

    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private Instant readAt;
    private Instant createdAt = Instant.now();

    public Notification() {
    }

    public Notification(Long userId, NotificationType type, String title, String message, String link,
            Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

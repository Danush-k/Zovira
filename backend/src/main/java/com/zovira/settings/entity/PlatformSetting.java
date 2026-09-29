package com.zovira.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "platform_settings")
public class PlatformSetting {

    @Id
    @Column(length = 60)
    private String key;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(length = 255)
    private String description;

    private Long updatedBy;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PlatformSetting() {
    }

    public void update(String value, Long updatedBy, Instant now) {
        this.value = value;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

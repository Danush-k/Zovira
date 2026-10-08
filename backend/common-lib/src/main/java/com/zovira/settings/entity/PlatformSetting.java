package com.zovira.settings.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "platform_settings")
public class PlatformSetting {

    @Id
    private String key;

    private String value;
    private String description;
    private Long updatedBy;
    private Instant updatedAt = Instant.now();

    public PlatformSetting() {
    }

    public PlatformSetting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void update(String value, Long updatedBy, Instant now) {
        this.value = value;
        this.updatedBy = updatedBy;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

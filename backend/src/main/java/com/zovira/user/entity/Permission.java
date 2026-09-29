package com.zovira.user.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    private String description;

    protected Permission() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

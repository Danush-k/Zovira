package com.zovira.user.entity;

import com.zovira.common.entity.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "roles")
public class Role extends BaseEntity {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String SELLER = "SELLER";
    public static final String ADMIN = "ADMIN";

    @Indexed(unique = true)
    private String name;

    private String description;

    private Set<Permission> permissions = new HashSet<>();

    public Role() {
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions == null ? new HashSet<>() : permissions;
    }
}

package com.zovira.security;

import com.zovira.user.entity.Role;
import java.util.Set;

/** The authenticated caller, resolved from the validated access token. */
public record AuthUser(Long id, String email, String name, Set<String> roles) {

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    public boolean isSeller() {
        return roles.contains(Role.SELLER);
    }
}

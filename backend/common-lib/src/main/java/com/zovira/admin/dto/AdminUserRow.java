package com.zovira.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminUserRow(
        Long id,
        String email,
        String fullName,
        String phone,
        String status,
        boolean emailVerified,
        List<String> roles,
        long orderCount,
        Instant lastLoginAt,
        Instant createdAt) {
}

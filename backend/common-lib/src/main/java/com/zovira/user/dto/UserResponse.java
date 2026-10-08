package com.zovira.user.dto;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        boolean emailVerified,
        List<String> roles,
        String sellerStatus,
        Instant createdAt) {
}

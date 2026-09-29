package com.zovira.auth.dto;

import java.time.Instant;

public record SessionResponse(Long id, String userAgent, String ipAddress, Instant createdAt, Instant lastUsedAt,
        boolean current) {
}

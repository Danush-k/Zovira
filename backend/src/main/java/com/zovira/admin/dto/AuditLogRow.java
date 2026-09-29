package com.zovira.admin.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogRow(Long id, String actorEmail, String action, String entityType, String entityId,
        Map<String, Object> details, String ipAddress, Instant createdAt) {
}

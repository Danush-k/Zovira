package com.zovira.settings.dto;

import java.time.Instant;

public record SettingResponse(String key, String value, String description, Instant updatedAt) {
}

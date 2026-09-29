package com.zovira.order.dto;

import java.time.Instant;

public record TimelineEntry(String status, String label, String note, Instant at) {
}

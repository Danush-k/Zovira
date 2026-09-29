package com.zovira.order.dto;

import java.util.List;

public record ReorderResponse(int added, List<String> unavailable) {
}

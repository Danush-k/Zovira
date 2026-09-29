package com.zovira.catalog.dto;

import com.zovira.catalog.entity.AttributeOption;
import java.util.List;

public record AttributeResponse(String name, List<AttributeOption> options) {
}

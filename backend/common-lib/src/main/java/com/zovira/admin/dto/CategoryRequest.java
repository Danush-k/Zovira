package com.zovira.admin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CategoryRequest(
        @NotBlank(message = "Enter a category name") @Size(max = 100) String name,
        Long parentId,
        @Size(max = 500) String description,
        @Size(max = 40) String icon,
        @Size(max = 512) String imageUrl,
        @NotNull @DecimalMin("0") @DecimalMax("40") BigDecimal taxRate,
        int sortOrder,
        boolean active) {
}

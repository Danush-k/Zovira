package com.zovira.seller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StockUpdateRequest(@NotEmpty @jakarta.validation.constraints.Size(max = 200) List<@Valid Row> rows) {

    public record Row(@NotNull Long variantId, @Min(0) @Max(100000) int available,
            @Min(0) @Max(1000) Integer lowStockThreshold) {
    }
}

package com.zovira.returns.dto;

import com.zovira.returns.entity.ReturnReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReturnRequest(
        @Min(1) @Max(10) int quantity,
        @NotNull(message = "Choose a reason") ReturnReason reason,
        @Size(max = 1000) String comments) {
}

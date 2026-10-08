package com.zovira.returns.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnDecisionRequest(@NotNull Decision decision, @Size(max = 500) String note) {

    public enum Decision {
        APPROVE,
        REJECT,
        RECEIVE
    }
}

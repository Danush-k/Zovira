package com.zovira.returns.entity;

public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    PICKED_UP,
    REFUNDED;

    public boolean isOpen() {
        return this == REQUESTED || this == APPROVED || this == PICKED_UP;
    }
}

package com.zovira.order.entity;

public enum PaymentMethod {
    UPI,
    CARD,
    NETBANKING,
    WALLET,
    COD;

    public boolean isOnline() {
        return this != COD;
    }
}

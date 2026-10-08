package com.zovira.coupon.entity;

public enum CouponScope {
    /** Applies to every item in the cart. */
    ALL,
    /** Applies only to items in the linked categories (or their sub-categories). */
    CATEGORY,
    /** Applies only to the linked products. */
    PRODUCT
}

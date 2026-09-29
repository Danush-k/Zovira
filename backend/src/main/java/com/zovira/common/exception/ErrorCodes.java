package com.zovira.common.exception;

/**
 * Stable error codes returned in the {@code code} field of every problem response.
 */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";

    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String QUANTITY_LIMIT = "QUANTITY_LIMIT";
    public static final String PRODUCT_UNAVAILABLE = "PRODUCT_UNAVAILABLE";
    public static final String CART_EMPTY = "CART_EMPTY";
    public static final String CART_HAS_ISSUES = "CART_HAS_ISSUES";
    public static final String COUPON_INVALID = "COUPON_INVALID";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String INVALID_FILE = "INVALID_FILE";

    private ErrorCodes() {
    }
}

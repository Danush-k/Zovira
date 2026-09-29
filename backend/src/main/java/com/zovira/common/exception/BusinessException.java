package com.zovira.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A request that is well-formed but violates a business rule (out of stock, invalid coupon,
 * illegal order transition, ...). Mapped to 422 unless a more specific status is given.
 */
public class BusinessException extends ApiException {

    public BusinessException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}

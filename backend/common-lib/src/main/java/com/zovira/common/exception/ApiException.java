package com.zovira.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all expected, client-facing failures. Carries an HTTP status and a stable,
 * machine-readable error code that frontends can switch on.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}

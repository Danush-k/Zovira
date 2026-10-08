package com.zovira.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND, resource + " not found");
    }

    public static NotFoundException of(String resource) {
        return new NotFoundException(resource);
    }
}

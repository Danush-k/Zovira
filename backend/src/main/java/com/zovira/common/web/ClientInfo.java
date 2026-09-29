package com.zovira.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Caller network metadata. The remote address is already resolved from X-Forwarded-For by the
 * framework's forwarded-header support, so only trusted proxy hops are honoured.
 */
public record ClientInfo(String ipAddress, String userAgent) {

    public static ClientInfo from(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 255) {
            ua = ua.substring(0, 255);
        }
        return new ClientInfo(request.getRemoteAddr(), ua);
    }
}

package com.zovira.auth.controller;

import com.zovira.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh token lives in an HttpOnly, SameSite cookie scoped to the auth endpoints, so it is
 * unreadable by scripts and never sent with ordinary API calls.
 */
@Component
public class RefreshCookies {

    public static final String NAME = "zv_rt";
    private static final String PATH = "/api/v1/auth";

    private final SecurityProperties properties;

    public RefreshCookies(SecurityProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie issue(String token) {
        return base(token).maxAge(properties.jwt().refreshTokenTtl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path(PATH);
    }
}

package com.zovira.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Security settings bound from {@code zovira.security.*}. Validated at startup so a missing or
 * weak JWT secret fails fast instead of producing forgeable tokens.
 */
@Validated
@ConfigurationProperties(prefix = "zovira.security")
public record SecurityProperties(
        @Valid @DefaultValue Jwt jwt,
        @Valid @DefaultValue Cors cors,
        @Valid @DefaultValue Cookie cookie,
        @Valid @DefaultValue RateLimit rateLimit,
        @Valid @DefaultValue Login login,
        @Valid @DefaultValue Password password) {

    public SecurityProperties {
        if (jwt == null) {
            jwt = new Jwt("dev-only-zovira-jwt-secret-change-me-0123456789abcdef", "zovira", Duration.ofMinutes(15), Duration.ofDays(30));
        }
        if (cors == null) {
            cors = new Cors(List.of("http://localhost:5173"));
        }
        if (cookie == null) {
            cookie = new Cookie(false, "Lax");
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(false);
        }
        if (login == null) {
            login = new Login(5, Duration.ofMinutes(15));
        }
        if (password == null) {
            password = new Password(12);
        }
    }

    public record Jwt(
            @NotBlank(message = "JWT_SECRET must be set")
            @Size(min = 32, message = "JWT_SECRET must be at least 32 characters")
            @DefaultValue("dev-only-zovira-jwt-secret-change-me-0123456789abcdef") String secret,
            @DefaultValue("zovira") String issuer,
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("30d") Duration refreshTokenTtl) {
    }

    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }

    public record Cookie(@DefaultValue("false") boolean secure, @DefaultValue("Lax") String sameSite) {
    }

    public record RateLimit(@DefaultValue("false") boolean enabled) {
    }

    public record Login(@Min(1) @DefaultValue("5") int maxFailedAttempts,
            @DefaultValue("15m") Duration lockDuration) {
    }

    public record Password(@Min(4) @DefaultValue("12") int bcryptStrength) {
    }
}

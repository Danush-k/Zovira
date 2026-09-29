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
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Cookie cookie,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Login login,
        @Valid @DefaultValue Password password) {

    public record Jwt(
            @NotBlank(message = "JWT_SECRET must be set")
            @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
            @DefaultValue("zovira") String issuer,
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("30d") Duration refreshTokenTtl) {
    }

    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }

    public record Cookie(@DefaultValue("true") boolean secure, @DefaultValue("Strict") String sameSite) {
    }

    public record RateLimit(@DefaultValue("true") boolean enabled) {
    }

    public record Login(@Min(1) @DefaultValue("5") int maxFailedAttempts,
            @DefaultValue("15m") Duration lockDuration) {
    }

    public record Password(@Min(4) @DefaultValue("12") int bcryptStrength) {
    }
}

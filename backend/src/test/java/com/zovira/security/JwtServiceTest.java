package com.zovira.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zovira.user.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private final SecurityProperties properties = new SecurityProperties(
            new SecurityProperties.Jwt("unit-test-secret-that-is-long-enough-123456", "zovira",
                    Duration.ofMinutes(15), Duration.ofDays(30)),
            new SecurityProperties.Cors(List.of("http://localhost:5173")),
            new SecurityProperties.Cookie(false, "Strict"),
            new SecurityProperties.RateLimit(false),
            new SecurityProperties.Login(5, Duration.ofMinutes(15)),
            new SecurityProperties.Password(4));

    private final JwtConfig config = new JwtConfig();
    private final SecretKey key = config.jwtSigningKey(properties);

    @Test
    void issuedTokenRoundTripsWithClaims() {
        JwtService service = new JwtService(config.jwtEncoder(key), properties, Clock.systemUTC());
        User user = new User("asha@example.com", "hash", "Asha Rao");
        ReflectionTestUtils.setField(user, "id", 42L);

        JwtService.AccessToken token = service.issueAccessToken(user);
        Jwt jwt = config.jwtDecoder(key, properties).decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("asha@example.com");
        assertThat(jwt.getClaimAsString("typ")).isEqualTo("access");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void expiredTokensAreRejected() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        JwtService service = new JwtService(config.jwtEncoder(key), properties, past);
        User user = new User("asha@example.com", "hash", "Asha Rao");
        ReflectionTestUtils.setField(user, "id", 1L);

        String token = service.issueAccessToken(user).value();
        JwtDecoder decoder = config.jwtDecoder(key, properties);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokensSignedWithAnotherKeyAreRejected() {
        SecurityProperties other = new SecurityProperties(
                new SecurityProperties.Jwt("a-completely-different-secret-9876543210", "zovira",
                        Duration.ofMinutes(15), Duration.ofDays(30)),
                properties.cors(), properties.cookie(), properties.rateLimit(), properties.login(),
                properties.password());
        JwtService forger = new JwtService(config.jwtEncoder(config.jwtSigningKey(other)), other, Clock.systemUTC());
        User user = new User("mallory@example.com", "hash", "Mallory");
        ReflectionTestUtils.setField(user, "id", 7L);

        String forged = forger.issueAccessToken(user).value();

        assertThatThrownBy(() -> config.jwtDecoder(key, properties).decode(forged)).isInstanceOf(JwtException.class);
    }
}

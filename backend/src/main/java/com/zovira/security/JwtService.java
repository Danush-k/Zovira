package com.zovira.security;

import com.zovira.user.entity.Permission;
import com.zovira.user.entity.Role;
import com.zovira.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived, signed access tokens carrying the user's roles and permissions. */
@Service
public class JwtService {

    public record AccessToken(String value, Instant expiresAt) {
    }

    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** The user must have roles and permissions initialised (see {@code findWithAuthoritiesById}). */
    public AccessToken issueAccessToken(User user) {
        // JWT NumericDate has second precision; truncate so the reported expiry matches the token exactly.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtConfig.CLAIM_TYPE, JwtConfig.ACCESS_TOKEN_TYPE)
                .claim(JwtConfig.CLAIM_EMAIL, user.getEmail())
                .claim(JwtConfig.CLAIM_NAME, user.getFullName())
                .claim(JwtConfig.CLAIM_ROLES, roles)
                .claim(JwtConfig.CLAIM_PERMISSIONS, permissions)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}

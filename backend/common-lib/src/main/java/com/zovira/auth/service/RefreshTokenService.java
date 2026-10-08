package com.zovira.auth.service;

import com.zovira.audit.service.AuditService;
import com.zovira.auth.entity.RefreshToken;
import com.zovira.auth.repository.RefreshTokenRepository;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.util.SecureTokens;
import com.zovira.common.web.ClientInfo;
import com.zovira.security.SecurityProperties;
import com.zovira.user.entity.User;
import com.zovira.user.entity.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque refresh tokens with rotation and reuse detection.
 *
 * <p>Each use revokes the presented token and issues a successor in the same family. Presenting a
 * token that was already rotated means it was copied, so the entire family is revoked. A short
 * grace window tolerates two tabs racing to refresh with the same token.
 */
@Service
public class RefreshTokenService {

    static final String REASON_ROTATED = "ROTATED";
    static final String REASON_REUSED = "REUSE_DETECTED";
    static final String REASON_LOGOUT = "LOGOUT";
    private static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    public record Rotation(User user, String token) {
    }

    private final RefreshTokenRepository repository;
    private final SecurityProperties properties;
    private final AuditService auditService;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, SecurityProperties properties,
            AuditService auditService, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public String issue(User user, UUID familyId, ClientInfo client) {
        String raw = SecureTokens.generate();
        Instant now = clock.instant();
        repository.save(new RefreshToken(user, SecureTokens.sha256(raw), familyId,
                now.plus(properties.jwt().refreshTokenTtl()), client.userAgent(), client.ipAddress(), now));
        return raw;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String raw, ClientInfo client) {
        Instant now = clock.instant();
        RefreshToken token = find(raw).orElseThrow(RefreshTokenService::invalid);

        if (token.getRevokedAt() != null) {
            boolean benignRace = REASON_ROTATED.equals(token.getRevokedReason())
                    && token.getRevokedAt().isAfter(now.minus(REUSE_GRACE));
            if (!benignRace) {
                repository.revokeFamily(token.getFamilyId(), now, REASON_REUSED);
                auditService.record(token.getUser().getId(), token.getUser().getEmail(), "REFRESH_TOKEN_REUSE",
                        "USER", token.getUser().getId(), Map.of("family", token.getFamilyId().toString()));
            }
            throw invalid();
        }
        if (!token.isActive(now)) {
            throw invalid();
        }
        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            repository.revokeFamily(token.getFamilyId(), now, "SUSPENDED");
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_SUSPENDED,
                    "This account has been suspended. Contact support for help.");
        }
        token.revoke(now, REASON_ROTATED);
        return new Rotation(user, issue(user, token.getFamilyId(), client));
    }

    @Transactional
    public void revoke(String raw) {
        find(raw).ifPresent(t -> repository.revokeFamily(t.getFamilyId(), clock.instant(), REASON_LOGOUT));
    }

    @Transactional
    public void revokeAll(Long userId, String reason) {
        repository.revokeAllForUser(userId, clock.instant(), reason);
    }

    /** Revokes every session of the user except the family the caller is currently using. */
    @Transactional
    public void revokeOthers(Long userId, String currentRaw, String reason) {
        UUID keep = find(currentRaw).map(RefreshToken::getFamilyId).orElse(null);
        Instant now = clock.instant();
        repository.findActiveByUser(userId, now).stream()
                .filter(t -> !t.getFamilyId().equals(keep))
                .forEach(t -> t.revoke(now, reason));
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> activeSessions(Long userId) {
        return repository.findActiveByUser(userId, clock.instant());
    }

    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        RefreshToken token = repository.findById(sessionId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND,
                        "Session not found"));
        repository.revokeFamily(token.getFamilyId(), clock.instant(), REASON_LOGOUT);
    }

    public Optional<RefreshToken> find(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 128) {
            return Optional.empty();
        }
        return repository.findByTokenHash(SecureTokens.sha256(raw));
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_TOKEN,
                "Your session has expired. Please sign in again.");
    }
}

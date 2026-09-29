package com.zovira.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zovira.audit.service.AuditService;
import com.zovira.auth.entity.RefreshToken;
import com.zovira.auth.repository.RefreshTokenRepository;
import com.zovira.common.exception.ApiException;
import com.zovira.common.util.SecureTokens;
import com.zovira.common.web.ClientInfo;
import com.zovira.security.SecurityProperties;
import com.zovira.user.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final ClientInfo CLIENT = new ClientInfo("127.0.0.1", "JUnit");

    @Mock
    private RefreshTokenRepository repository;
    @Mock
    private AuditService auditService;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("unit-test-secret-that-is-long-enough-123456", "zovira",
                        Duration.ofMinutes(15), Duration.ofDays(30)),
                new SecurityProperties.Cors(List.of()), new SecurityProperties.Cookie(false, "Strict"),
                new SecurityProperties.RateLimit(false), new SecurityProperties.Login(5, Duration.ofMinutes(15)),
                new SecurityProperties.Password(4));
        service = new RefreshTokenService(repository, properties, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User("asha@example.com", "hash", "Asha Rao");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    private RefreshToken stored(String raw, UUID family, Instant expiresAt) {
        RefreshToken token = new RefreshToken(user, SecureTokens.sha256(raw), family, expiresAt, "ua", "ip",
                NOW.minus(Duration.ofDays(1)));
        when(repository.findByTokenHash(SecureTokens.sha256(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void rotationRevokesPresentedTokenAndIssuesSuccessorInSameFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken current = stored("raw-token", family, NOW.plus(Duration.ofDays(10)));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate("raw-token", CLIENT);

        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(current.getRevokedReason()).isEqualTo(RefreshTokenService.REASON_ROTATED);
        assertThat(rotation.token()).isNotEqualTo("raw-token");
        assertThat(rotation.user()).isSameAs(user);
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(t -> t.getFamilyId().equals(family)));
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken stolen = stored("stolen", family, NOW.plus(Duration.ofDays(10)));
        stolen.revoke(NOW.minus(Duration.ofMinutes(5)), RefreshTokenService.REASON_ROTATED);

        assertThatThrownBy(() -> service.rotate("stolen", CLIENT)).isInstanceOf(ApiException.class);

        verify(repository).revokeFamily(family, NOW, RefreshTokenService.REASON_REUSED);
        verify(auditService).record(eq(1L), anyString(), eq("REFRESH_TOKEN_REUSE"), eq("USER"), eq(1L), anyMap());
    }

    @Test
    void concurrentRefreshWithinGraceWindowDoesNotRevokeFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken justRotated = stored("racing", family, NOW.plus(Duration.ofDays(10)));
        justRotated.revoke(NOW.minus(Duration.ofSeconds(5)), RefreshTokenService.REASON_ROTATED);

        assertThatThrownBy(() -> service.rotate("racing", CLIENT)).isInstanceOf(ApiException.class);

        verify(repository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void expiredTokensAreRejected() {
        stored("old", UUID.randomUUID(), NOW.minus(Duration.ofSeconds(1)));

        assertThatThrownBy(() -> service.rotate("old", CLIENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void unknownTokensAreRejected() {
        assertThatThrownBy(() -> service.rotate("does-not-exist", CLIENT)).isInstanceOf(ApiException.class);
    }
}

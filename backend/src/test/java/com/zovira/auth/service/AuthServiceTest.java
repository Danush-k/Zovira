package com.zovira.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zovira.audit.service.AuditService;
import com.zovira.auth.dto.LoginRequest;
import com.zovira.auth.dto.RegisterRequest;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.web.ClientInfo;
import com.zovira.security.JwtService;
import com.zovira.security.SecurityProperties;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.user.entity.User;
import com.zovira.user.entity.UserStatus;
import com.zovira.user.repository.RoleRepository;
import com.zovira.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final ClientInfo CLIENT = new ClientInfo("127.0.0.1", "JUnit");

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AccountTokenService accountTokenService;
    @Mock
    private AuditService auditService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService service;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("unit-test-secret-that-is-long-enough-123456", "zovira",
                        Duration.ofMinutes(15), Duration.ofDays(30)),
                new SecurityProperties.Cors(List.of()), new SecurityProperties.Cookie(false, "Strict"),
                new SecurityProperties.RateLimit(false), new SecurityProperties.Login(3, Duration.ofMinutes(15)),
                new SecurityProperties.Password(4));
        service = new AuthService(userRepository, roleRepository, sellerRepository, encoder, jwtService,
                refreshTokenService, accountTokenService, auditService, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User existingUser(String password) {
        User user = new User("asha@example.com", encoder.encode(password), "Asha Rao");
        ReflectionTestUtils.setField(user, "id", 5L);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void unknownEmailFailsWithGenericCredentialsError() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "Secret123"), CLIENT))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCode()).isEqualTo(ErrorCodes.INVALID_CREDENTIALS);
                });
    }

    @Test
    void repeatedFailuresLockTheAccount() {
        User user = existingUser("Correct123");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("asha@example.com", "Wrong1234"), CLIENT))
                    .isInstanceOf(ApiException.class);
        }

        assertThat(user.isLocked(NOW)).isTrue();
        assertThatThrownBy(() -> service.login(new LoginRequest("asha@example.com", "Correct123"), CLIENT))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED));
    }

    @Test
    void suspendedAccountsCannotSignIn() {
        User user = existingUser("Correct123");
        user.setStatus(UserStatus.SUSPENDED);

        assertThatThrownBy(() -> service.login(new LoginRequest("asha@example.com", "Correct123"), CLIENT))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCodes.ACCOUNT_SUSPENDED));
        verify(refreshTokenService, never()).issue(any(), any(), any());
    }

    @Test
    void registrationRejectsDuplicateEmailsCaseInsensitively() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Asha Rao", "  Asha@Example.com ", "Secret123"), CLIENT))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCodes.EMAIL_TAKEN));
    }
}

package com.zovira.auth.service;

import com.zovira.audit.service.AuditService;
import com.zovira.auth.dto.LoginRequest;
import com.zovira.auth.dto.RegisterRequest;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.web.ClientInfo;
import com.zovira.security.JwtService;
import com.zovira.security.SecurityProperties;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.user.dto.UserResponse;
import com.zovira.user.entity.Role;
import com.zovira.user.entity.User;
import com.zovira.user.entity.UserStatus;
import com.zovira.user.mapper.UserMapper;
import com.zovira.user.repository.RoleRepository;
import com.zovira.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public record AuthResult(UserResponse user, JwtService.AccessToken accessToken, String refreshToken) {
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AccountTokenService accountTokenService;
    private final AuditService auditService;
    private final SecurityProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
            SellerRepository sellerRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            RefreshTokenService refreshTokenService, AccountTokenService accountTokenService,
            AuditService auditService, SecurityProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.accountTokenService = accountTokenService;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
        // Compared against when the email is unknown so response timing does not reveal account existence.
        this.dummyHash = passwordEncoder.encode("timing-equalizer-" + UUID.randomUUID());
    }

    @Transactional
    public AuthResult register(RegisterRequest request, ClientInfo client) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.EMAIL_TAKEN,
                    "An account with this email already exists. Try signing in instead.");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim());
        user.getRoles().add(roleRepository.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role missing")));
        user.recordSuccessfulLogin(clock.instant());
        userRepository.save(user);

        accountTokenService.sendEmailVerification(user);
        auditService.record(user.getId(), user.getEmail(), "USER_REGISTERED", "USER", user.getId(), Map.of());
        return startSession(user.getId(), client);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult login(LoginRequest request, ClientInfo client) {
        Instant now = clock.instant();
        User user = userRepository.findByEmail(User.normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        if (user.isLocked(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw new ApiException(HttpStatus.LOCKED, ErrorCodes.ACCOUNT_LOCKED,
                    "Too many failed attempts. Try again in " + minutes + " minute" + (minutes == 1 ? "" : "s")
                            + " or reset your password.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(properties.login().maxFailedAttempts(), now.plus(properties.login().lockDuration()));
            if (user.isLocked(now)) {
                auditService.record(user.getId(), user.getEmail(), "ACCOUNT_LOCKED", "USER", user.getId(),
                        Map.of("ip", String.valueOf(client.ipAddress())));
            }
            throw invalidCredentials();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_SUSPENDED,
                    "This account has been suspended. Contact support for help.");
        }
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.recordSuccessfulLogin(now);
        return startSession(user.getId(), client);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String refreshToken, ClientInfo client) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken, client);
        User user = userRepository.findWithAuthoritiesById(rotation.user().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_TOKEN,
                        "Your session has expired. Please sign in again."));
        return new AuthResult(toResponse(user), jwtService.issueAccessToken(user), rotation.token());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public void logoutEverywhere(Long userId) {
        refreshTokenService.revokeAll(userId, "LOGOUT_ALL");
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findWithAuthoritiesById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED,
                        "Authentication is required"));
        return toResponse(user);
    }

    private AuthResult startSession(Long userId, ClientInfo client) {
        User user = userRepository.findWithAuthoritiesById(userId).orElseThrow();
        String refresh = refreshTokenService.issue(user, UUID.randomUUID(), client);
        return new AuthResult(toResponse(user), jwtService.issueAccessToken(user), refresh);
    }

    public UserResponse toResponse(User user) {
        SellerStatus sellerStatus = sellerRepository.findByUserId(user.getId()).map(Seller::getStatus).orElse(null);
        return UserMapper.toResponse(user, sellerStatus);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS,
                "The email or password you entered is incorrect.");
    }
}

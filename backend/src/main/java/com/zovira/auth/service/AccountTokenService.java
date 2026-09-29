package com.zovira.auth.service;

import com.zovira.audit.service.AuditService;
import com.zovira.auth.entity.UserToken;
import com.zovira.auth.entity.UserTokenType;
import com.zovira.auth.repository.UserTokenRepository;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.util.SecureTokens;
import com.zovira.mail.EmailTemplates;
import com.zovira.mail.MailService;
import com.zovira.user.entity.User;
import com.zovira.user.entity.UserStatus;
import com.zovira.user.repository.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email verification and password reset flows built on single-use hashed tokens. */
@Service
public class AccountTokenService {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;
    private final Clock clock;
    private final String frontendUrl;

    public AccountTokenService(UserTokenRepository tokenRepository, UserRepository userRepository,
            RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder, MailService mailService,
            AuditService auditService, Clock clock, @Value("${zovira.app.frontend-url}") String frontendUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.auditService = auditService;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void sendEmailVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        String raw = createToken(user, UserTokenType.EMAIL_VERIFICATION, VERIFICATION_TTL);
        mailService.send(EmailTemplates.verifyEmail(user.getEmail(), user.getFullName(),
                frontendUrl + "/verify-email?token=" + encode(raw)));
    }

    @Transactional
    public void verifyEmail(String raw) {
        UserToken token = consume(raw, UserTokenType.EMAIL_VERIFICATION);
        token.getUser().setEmailVerified(true);
    }

    /** Always completes silently so the endpoint cannot be used to discover registered emails. */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(User.normalizeEmail(email))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    String raw = createToken(user, UserTokenType.PASSWORD_RESET, RESET_TTL);
                    mailService.send(EmailTemplates.resetPassword(user.getEmail(), user.getFullName(),
                            frontendUrl + "/reset-password?token=" + encode(raw)));
                });
    }

    @Transactional
    public void resetPassword(String raw, String newPassword) {
        UserToken token = consume(raw, UserTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.clearLockout();
        // A reset proves inbox ownership, so the address is effectively verified.
        user.setEmailVerified(true);
        refreshTokenService.revokeAll(user.getId(), "PASSWORD_RESET");
        auditService.record(user.getId(), user.getEmail(), "PASSWORD_RESET", "USER", user.getId(), Map.of());
        mailService.send(EmailTemplates.passwordChanged(user.getEmail(), user.getFullName()));
    }

    private String createToken(User user, UserTokenType type, Duration ttl) {
        Instant now = clock.instant();
        tokenRepository.invalidateOutstanding(user.getId(), type, now);
        String raw = SecureTokens.generate();
        tokenRepository.save(new UserToken(user, type, SecureTokens.sha256(raw), now.plus(ttl), now));
        return raw;
    }

    private UserToken consume(String raw, UserTokenType type) {
        Instant now = clock.instant();
        UserToken token = tokenRepository.findByHashAndType(SecureTokens.sha256(raw), type)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_TOKEN,
                        "This link is invalid or has expired. Please request a new one."));
        token.markUsed(now);
        return token;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

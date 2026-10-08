package com.zovira.auth.controller;

import com.zovira.auth.dto.AuthResponse;
import com.zovira.auth.dto.ForgotPasswordRequest;
import com.zovira.auth.dto.LoginRequest;
import com.zovira.auth.dto.RegisterRequest;
import com.zovira.auth.dto.ResetPasswordRequest;
import com.zovira.auth.dto.SessionResponse;
import com.zovira.auth.dto.TokenRequest;
import com.zovira.auth.entity.RefreshToken;
import com.zovira.auth.service.AccountTokenService;
import com.zovira.auth.service.AuthService;
import com.zovira.auth.service.RefreshTokenService;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.GlobalExceptionHandler;
import com.zovira.common.web.ClientInfo;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.security.SecurityProperties;
import com.zovira.user.entity.User;
import com.zovira.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, sign-in, token refresh and account recovery")
public class AuthController {

    private final AuthService authService;
    private final AccountTokenService accountTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final RefreshCookies cookies;
    private final SecurityProperties properties;

    public AuthController(AuthService authService, AccountTokenService accountTokenService,
            RefreshTokenService refreshTokenService, UserRepository userRepository, RefreshCookies cookies,
            SecurityProperties properties) {
        this.authService = authService;
        this.accountTokenService = accountTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.cookies = cookies;
        this.properties = properties;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a customer account and start a session")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest http) {
        return session(HttpStatus.CREATED, authService.register(request, ClientInfo.from(http)));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return session(HttpStatus.OK, authService.login(request, ClientInfo.from(http)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh cookie for a new access token (rotates the cookie)")
    public ResponseEntity<?> refresh(HttpServletRequest http) {
        requireTrustedOrigin(http);
        String token = cookies.read(http);
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_TOKEN, "No active session");
        }
        try {
            return session(HttpStatus.OK, authService.refresh(token, ClientInfo.from(http)));
        } catch (ApiException e) {
            // Drop the dead cookie so the browser stops presenting it.
            return ResponseEntity.status(e.getStatus())
                    .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                    .body(GlobalExceptionHandler.problem(e.getStatus(), e.getCode(), e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "End the current session")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        requireTrustedOrigin(http);
        authService.logout(cookies.read(http));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Sign out of every device")
    public ResponseEntity<Void> logoutAll(@CurrentUser AuthUser user) {
        authService.logoutEverywhere(user.id());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirm an email address using the emailed token")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        accountTokenService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Send a fresh verification email")
    public ResponseEntity<Void> resendVerification(@CurrentUser AuthUser user) {
        User entity = userRepository.findById(user.id()).orElseThrow();
        accountTokenService.sendEmailVerification(entity);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a password reset link if the account exists")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        accountTokenService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using the emailed token")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountTokenService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    @Operation(summary = "List devices currently signed in")
    public List<SessionResponse> sessions(@CurrentUser AuthUser user, HttpServletRequest http) {
        UUID currentFamily = refreshTokenService.find(cookies.read(http))
                .map(RefreshToken::getFamilyId).orElse(null);
        return refreshTokenService.activeSessions(user.id()).stream()
                .map(t -> new SessionResponse(t.getId(), t.getUserAgent(), t.getIpAddress(), t.getCreatedAt(),
                        t.getLastUsedAt(), t.getFamilyId().equals(currentFamily)))
                .toList();
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Sign out a specific device")
    public ResponseEntity<Void> revokeSession(@CurrentUser AuthUser user, @PathVariable Long id) {
        refreshTokenService.revokeSession(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> session(HttpStatus status, AuthService.AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.issue(result.refreshToken()).toString())
                .body(AuthResponse.bearer(result.accessToken().value(), result.accessToken().expiresAt(),
                        result.user()));
    }

    /**
     * Defence in depth for the cookie-authenticated endpoints: SameSite already blocks cross-site
     * sends, and this rejects any browser request whose Origin is neither this host nor an
     * explicitly allowed web origin.
     */
    private void requireTrustedOrigin(HttpServletRequest http) {
        String origin = http.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return;
        }
        boolean allowed = properties.cors().allowedOrigins().contains(origin)
                || Optional.of(URI.create(origin))
                        .map(uri -> uri.getHost() != null && uri.getHost().equalsIgnoreCase(http.getServerName()))
                        .orElse(false);
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Request origin not allowed");
        }
    }
}

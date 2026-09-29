package com.zovira.user.service;

import com.zovira.audit.service.AuditService;
import com.zovira.auth.service.AuthService;
import com.zovira.auth.service.RefreshTokenService;
import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.mail.EmailTemplates;
import com.zovira.mail.MailService;
import com.zovira.user.dto.ChangePasswordRequest;
import com.zovira.user.dto.UpdateProfileRequest;
import com.zovira.user.dto.UserResponse;
import com.zovira.user.entity.User;
import com.zovira.user.repository.UserRepository;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final AuditService auditService;
    private final MailService mailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService, AuthService authService, AuditService auditService,
            MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.authService = authService;
        this.auditService = auditService;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public UserResponse profile(Long userId) {
        return authService.currentUser(userId);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone());
        return authService.currentUser(userId);
    }

    /** Verifies the current password, then signs out every other device. */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, String currentRefreshToken) {
        User user = load(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_CREDENTIALS,
                    "Your current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                    "Choose a password you haven't used for this account.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeOthers(userId, currentRefreshToken, "PASSWORD_CHANGED");
        auditService.record(user.getId(), user.getEmail(), "PASSWORD_CHANGED", "USER", user.getId(), Map.of());
        mailService.send(EmailTemplates.passwordChanged(user.getEmail(), user.getFullName()));
    }

    @Transactional
    public UserResponse updateAvatar(Long userId, String url) {
        load(userId).setAvatarUrl(url);
        return authService.currentUser(userId);
    }

    private User load(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User"));
    }
}

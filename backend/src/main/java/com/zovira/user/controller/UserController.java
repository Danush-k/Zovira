package com.zovira.user.controller;

import com.zovira.auth.controller.RefreshCookies;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.user.dto.ChangePasswordRequest;
import com.zovira.user.dto.UpdateProfileRequest;
import com.zovira.user.dto.UserResponse;
import com.zovira.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Account", description = "The signed-in user's profile and credentials")
public class UserController {

    private final UserService userService;
    private final RefreshCookies cookies;

    public UserController(UserService userService, RefreshCookies cookies) {
        this.userService = userService;
        this.cookies = cookies;
    }

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public UserResponse me(@CurrentUser AuthUser user) {
        return userService.profile(user.id());
    }

    @PatchMapping
    @Operation(summary = "Update name and phone number")
    public UserResponse update(@CurrentUser AuthUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.id(), request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password and sign out other devices")
    public ResponseEntity<Void> changePassword(@CurrentUser AuthUser user,
            @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        userService.changePassword(user.id(), request, cookies.read(http));
        return ResponseEntity.noContent().build();
    }
}

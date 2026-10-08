package com.zovira.user.dto;

import com.zovira.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Enter your current password") @Size(max = 128) String currentPassword,
        @StrongPassword String newPassword) {
}

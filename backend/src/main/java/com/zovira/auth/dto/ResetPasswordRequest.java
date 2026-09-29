package com.zovira.auth.dto;

import com.zovira.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank @Size(max = 128) String token, @StrongPassword String password) {
}

package com.zovira.auth.dto;

import com.zovira.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your full name")
        @Size(min = 2, max = 120, message = "Name must be 2 to 120 characters") String fullName,
        @NotBlank(message = "Enter your email address") @Email(message = "Enter a valid email address")
        @Size(max = 254) String email,
        @StrongPassword String password) {
}

package com.zovira.user.dto;

import com.zovira.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Enter your full name")
        @Size(min = 2, max = 120, message = "Name must be 2 to 120 characters") String fullName,
        @Pattern(regexp = ValidationPatterns.INDIAN_MOBILE, message = "Enter a valid 10-digit mobile number")
        String phone) {
}

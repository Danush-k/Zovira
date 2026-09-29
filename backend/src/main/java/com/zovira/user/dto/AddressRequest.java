package com.zovira.user.dto;

import com.zovira.common.validation.ValidationPatterns;
import com.zovira.user.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Enter the recipient's name") @Size(max = 120) String fullName,
        @NotBlank(message = "Enter a mobile number")
        @Pattern(regexp = ValidationPatterns.INDIAN_MOBILE, message = "Enter a valid 10-digit mobile number")
        String phone,
        @NotBlank(message = "Enter the house number and street") @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @Size(max = 120) String landmark,
        @NotBlank(message = "Enter a city") @Size(max = 80) String city,
        @NotBlank(message = "Select a state") @Size(max = 80) String state,
        @NotBlank(message = "Enter a PIN code")
        @Pattern(regexp = ValidationPatterns.PINCODE, message = "Enter a valid 6-digit PIN code") String pincode,
        @NotNull AddressType type,
        boolean makeDefault) {
}

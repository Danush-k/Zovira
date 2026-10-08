package com.zovira.seller.dto;

import com.zovira.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
        @NotBlank(message = "Enter your store name") @Size(min = 3, max = 120) String storeName,
        @Size(max = 1000) String description,
        @Pattern(regexp = ValidationPatterns.GSTIN, message = "Enter a valid 15-character GSTIN") String gstin,
        @NotBlank(message = "Enter a support email") @jakarta.validation.constraints.Email @Size(max = 254) String supportEmail,
        @NotBlank(message = "Enter a support phone number")
        @Pattern(regexp = ValidationPatterns.INDIAN_MOBILE, message = "Enter a valid 10-digit mobile number") String supportPhone,
        @NotBlank(message = "Enter the pickup address") @Size(max = 200) String pickupLine1,
        @NotBlank(message = "Enter a city") @Size(max = 80) String pickupCity,
        @NotBlank(message = "Select a state") @Size(max = 80) String pickupState,
        @NotBlank(message = "Enter a PIN code")
        @Pattern(regexp = ValidationPatterns.PINCODE, message = "Enter a valid 6-digit PIN code") String pickupPincode) {
}

package com.zovira.seller.controller;

import com.zovira.seller.dto.SellerApplicationRequest;
import com.zovira.seller.dto.SellerProfileResponse;
import com.zovira.seller.service.SellerOnboardingService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Open to any signed-in customer: applying to sell and checking application status. */
@RestController
@RequestMapping("/api/v1/sell")
@Tag(name = "Seller onboarding", description = "Applying to sell on Zovira")
public class SellerOnboardingController {

    private final SellerOnboardingService onboarding;

    public SellerOnboardingController(SellerOnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @GetMapping("/application")
    @Operation(summary = "The signed-in user's seller application, if any")
    public ResponseEntity<SellerProfileResponse> application(@CurrentUser AuthUser user) {
        return onboarding.mine(user.id()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/application")
    @Operation(summary = "Apply to sell on Zovira")
    public ResponseEntity<SellerProfileResponse> apply(@CurrentUser AuthUser user,
            @Valid @RequestBody SellerApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboarding.apply(user.id(), request));
    }
}

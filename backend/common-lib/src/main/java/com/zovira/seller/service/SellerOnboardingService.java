package com.zovira.seller.service;

import com.zovira.audit.service.AuditService;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.util.Slugs;
import com.zovira.seller.dto.SellerApplicationRequest;
import com.zovira.seller.dto.SellerProfileResponse;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.user.entity.User;
import com.zovira.user.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerOnboardingService {

    private final SellerRepository sellers;
    private final UserRepository users;
    private final AuditService audit;

    public SellerOnboardingService(SellerRepository sellers, UserRepository users, AuditService audit) {
        this.sellers = sellers;
        this.users = users;
        this.audit = audit;
    }

    /** The seller record for this user, whatever its state, so the UI can show application status. */
    @Transactional(readOnly = true)
    public Optional<SellerProfileResponse> mine(Long userId) {
        return sellers.findByUserId(userId).map(SellerOnboardingService::toResponse);
    }

    @Transactional
    public SellerProfileResponse apply(Long userId, SellerApplicationRequest request) {
        if (sellers.findByUserId(userId).isPresent()) {
            throw new BusinessException(ErrorCodes.CONFLICT, "You've already applied to sell on Zovira.");
        }
        User user = users.findById(userId).orElseThrow();
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCodes.EMAIL_NOT_VERIFIED, "Verify your email address before applying.");
        }
        Seller seller = new Seller(user, request.storeName().trim(),
                Slugs.unique(request.storeName(), sellers::existsBySlug));
        apply(seller, request);
        sellers.save(seller);
        audit.record("SELLER_APPLIED", "SELLER", seller.getId(), Map.of("storeName", seller.getStoreName()));
        return toResponse(seller);
    }

    /** Sellers may edit their storefront details; approval status and slug are not theirs to change. */
    @Transactional
    public SellerProfileResponse update(Long sellerId, SellerApplicationRequest request) {
        Seller seller = sellers.findById(sellerId).orElseThrow();
        seller.setStoreName(request.storeName().trim());
        apply(seller, request);
        return toResponse(seller);
    }

    private static void apply(Seller seller, SellerApplicationRequest r) {
        seller.setDescription(blankToNull(r.description()));
        seller.setGstin(blankToNull(r.gstin()));
        seller.setSupportEmail(r.supportEmail().trim());
        seller.setSupportPhone(r.supportPhone());
        seller.setPickupLine1(r.pickupLine1().trim());
        seller.setPickupCity(r.pickupCity().trim());
        seller.setPickupState(r.pickupState().trim());
        seller.setPickupPincode(r.pickupPincode());
    }

    public static SellerProfileResponse toResponse(Seller s) {
        return new SellerProfileResponse(s.getId(), s.getStoreName(), s.getSlug(), s.getDescription(), s.getLogoUrl(),
                s.getGstin(), s.getSupportEmail(), s.getSupportPhone(), s.getPickupLine1(), s.getPickupCity(),
                s.getPickupState(), s.getPickupPincode(), s.getStatus().name(), s.getStatusReason(),
                s.getRatingAverage(), s.getRatingCount(), s.getApprovedAt(), s.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

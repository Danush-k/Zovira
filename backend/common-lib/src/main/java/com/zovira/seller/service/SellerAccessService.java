package com.zovira.seller.service;

import com.zovira.common.exception.ApiException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.seller.repository.SellerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the seller account behind the signed-in user. Every seller endpoint goes through here,
 * so a suspended or unapproved seller cannot reach seller data, and one seller's id can never be
 * substituted for another's.
 */
@Service
public class SellerAccessService {

    private final SellerRepository sellers;

    public SellerAccessService(SellerRepository sellers) {
        this.sellers = sellers;
    }

    @Transactional(readOnly = true)
    public Seller requireApproved(Long userId) {
        Seller seller = sellers.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                        "You don't have a seller account yet."));
        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, switch (seller.getStatus()) {
                case PENDING -> "Your seller application is still under review.";
                case REJECTED -> "Your seller application was not approved.";
                default -> "Your seller account is suspended. Contact support for help.";
            });
        }
        return seller;
    }

    @Transactional(readOnly = true)
    public Long requireApprovedId(Long userId) {
        return requireApproved(userId).getId();
    }
}

package com.zovira.catalog.service;

import com.zovira.catalog.dto.StoreResponse;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.mapper.CatalogMapper;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.NotFoundException;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.repository.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public seller storefront information. */
@Service
public class StoreService {

    private final SellerRepository sellers;
    private final ProductRepository products;

    public StoreService(SellerRepository sellers, ProductRepository products) {
        this.sellers = sellers;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public StoreResponse store(String slug) {
        Seller seller = sellers.findBySlug(slug).filter(Seller::isApproved)
                .orElseThrow(() -> NotFoundException.of("Store"));
        return new StoreResponse(CatalogMapper.toSellerSummary(seller), seller.getDescription(), seller.getLogoUrl(),
                seller.getApprovedAt() != null ? seller.getApprovedAt() : seller.getCreatedAt(),
                products.countBySellerIdAndStatus(seller.getId(), ProductStatus.ACTIVE));
    }
}

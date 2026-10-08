package com.zovira.review.service;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.service.ProductDetailAssembler;
import com.zovira.review.repository.ReviewRepository;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.repository.SellerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the denormalized rating on products and sellers in step with published reviews, so
 * listing pages never aggregate at read time.
 */
@Component
public class ReviewRatingUpdater {

    private final ReviewRepository reviews;
    private final SellerRepository sellers;
    private final ProductDetailAssembler assembler;

    public ReviewRatingUpdater(ReviewRepository reviews, SellerRepository sellers, ProductDetailAssembler assembler) {
        this.reviews = reviews;
        this.sellers = sellers;
        this.assembler = assembler;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refresh(Product product) {
        Object[] summary = (Object[]) reviews.ratingSummary(product.getId())[0];
        product.updateRating(round(summary[0]), ((Number) summary[1]).intValue());
        assembler.evict(product.getSlug());

        Seller seller = product.getSeller();
        Object[] sellerSummary = (Object[]) reviews.sellerRatingSummary(seller.getId())[0];
        sellers.findById(seller.getId()).ifPresent(s ->
                s.updateRating(round(sellerSummary[0]), ((Number) sellerSummary[1]).intValue()));
    }

    private static BigDecimal round(Object value) {
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}

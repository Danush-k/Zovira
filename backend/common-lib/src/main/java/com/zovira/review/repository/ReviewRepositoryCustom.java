package com.zovira.review.repository;

import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ReviewRepositoryCustom {

    Page<Review> findPublished(Long productId, ReviewStatus status, Integer rating, boolean withPhotos,
            Pageable pageable);

    List<Object[]> ratingBreakdown(Long productId);

    Object[] ratingSummary(Long productId);

    Object[] sellerRatingSummary(Long sellerId);
}

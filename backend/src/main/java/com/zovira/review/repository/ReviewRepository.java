package com.zovira.review.repository;

import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    long countByStatus(ReviewStatus status);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    @Query("""
            select r from Review r
            join fetch r.user
            where r.product.id = :productId and r.status = :status
              and (:rating is null or r.rating = :rating)
              and (:withPhotos = false or size(r.images) > 0)
            """)
    Page<Review> findPublished(@Param("productId") Long productId, @Param("status") ReviewStatus status,
            @Param("rating") Integer rating, @Param("withPhotos") boolean withPhotos, Pageable pageable);

    @Query("select r.rating, count(r) from Review r where r.product.id = :productId and r.status = 'PUBLISHED' group by r.rating")
    java.util.List<Object[]> ratingBreakdown(@Param("productId") Long productId);

    @Query("""
            select coalesce(avg(r.rating), 0), count(r) from Review r
            where r.product.id = :productId and r.status = 'PUBLISHED'
            """)
    Object[] ratingSummary(@Param("productId") Long productId);

    @Query("""
            select coalesce(avg(r.rating), 0), count(r) from Review r
            where r.product.seller.id = :sellerId and r.status = 'PUBLISHED'
            """)
    Object[] sellerRatingSummary(@Param("sellerId") Long sellerId);
}

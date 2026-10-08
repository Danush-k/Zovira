package com.zovira.review.repository;

import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewStatus;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewRepository extends MongoRepository<Review, Long>, ReviewRepositoryCustom {

    long countByStatus(ReviewStatus status);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
}

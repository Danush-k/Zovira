package com.zovira.review.repository;

import com.zovira.catalog.entity.Product;
import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ReviewRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Review> findPublished(Long productId, ReviewStatus status, Integer rating, boolean withPhotos,
            Pageable pageable) {
        Criteria criteria = Criteria.where("productId").is(productId)
                .and("status").is(status != null ? status : ReviewStatus.PUBLISHED);

        if (rating != null) {
            criteria.and("rating").is(rating);
        }
        if (withPhotos) {
            criteria.and("images.0").exists(true);
        }

        long total = mongoTemplate.count(new Query(criteria), Review.class);
        List<Review> content = mongoTemplate.find(new Query(criteria).with(pageable), Review.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Object[]> ratingBreakdown(Long productId) {
        Query query = new Query(Criteria.where("productId").is(productId)
                .and("status").is(ReviewStatus.PUBLISHED));
        List<Review> reviews = mongoTemplate.find(query, Review.class);
        Map<Integer, Long> counts = new HashMap<>();
        for (Review r : reviews) {
            counts.put(r.getRating(), counts.getOrDefault(r.getRating(), 0L) + 1);
        }
        List<Object[]> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : counts.entrySet()) {
            result.add(new Object[]{e.getKey(), e.getValue()});
        }
        return result;
    }

    @Override
    public Object[] ratingSummary(Long productId) {
        Query query = new Query(Criteria.where("productId").is(productId)
                .and("status").is(ReviewStatus.PUBLISHED));
        List<Review> reviews = mongoTemplate.find(query, Review.class);
        if (reviews.isEmpty()) {
            return new Object[]{0.0, 0L};
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new Object[]{avg, (long) reviews.size()};
    }

    @Override
    public Object[] sellerRatingSummary(Long sellerId) {
        Query pQuery = new Query(Criteria.where("sellerId").is(sellerId));
        List<Product> products = mongoTemplate.find(pQuery, Product.class);
        List<Long> pIds = products.stream().map(Product::getId).toList();
        if (pIds.isEmpty()) {
            return new Object[]{0.0, 0L};
        }
        Query rQuery = new Query(Criteria.where("productId").in(pIds).and("status").is(ReviewStatus.PUBLISHED));
        List<Review> reviews = mongoTemplate.find(rQuery, Review.class);
        if (reviews.isEmpty()) {
            return new Object[]{0.0, 0L};
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new Object[]{avg, (long) reviews.size()};
    }
}

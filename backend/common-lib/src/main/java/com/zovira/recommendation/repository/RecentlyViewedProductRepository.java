package com.zovira.recommendation.repository;

import com.zovira.recommendation.entity.RecentlyViewedProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecentlyViewedProductRepository extends MongoRepository<RecentlyViewedProduct, Long> {
}

package com.zovira.recommendation.repository;


import com.zovira.recommendation.entity.RecentlyViewedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, Long> {
}

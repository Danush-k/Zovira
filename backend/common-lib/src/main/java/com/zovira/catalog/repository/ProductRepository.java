package com.zovira.catalog.repository;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, Long>, ProductRepositoryCustom {

    Optional<Product> findBySlug(String slug);

    default Optional<Product> findDetailBySlug(String slug) {
        return findBySlug(slug);
    }

    boolean existsBySlug(String slug);

    List<Product> findByIdIn(Collection<Long> ids);

    default List<Product> findSummariesByIdIn(Collection<Long> ids) {
        return findByIdIn(ids);
    }

    long countBySellerIdAndStatus(Long sellerId, ProductStatus status);

    List<Product> findBySellerId(Long sellerId, org.springframework.data.domain.Sort sort);

    List<Product> findBySellerId(Long sellerId);
}

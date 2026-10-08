package com.zovira.catalog.repository;

import com.zovira.catalog.entity.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductVariantRepository extends MongoRepository<ProductVariant, Long> {

    default Optional<ProductVariant> findWithProductById(Long id) {
        return findById(id);
    }

    List<ProductVariant> findByIdIn(Collection<Long> ids);

    default List<ProductVariant> findWithProductByIdIn(Collection<Long> ids) {
        return findByIdIn(ids);
    }

    boolean existsBySku(String sku);
}

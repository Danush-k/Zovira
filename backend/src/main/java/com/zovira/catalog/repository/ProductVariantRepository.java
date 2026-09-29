package com.zovira.catalog.repository;

import com.zovira.catalog.entity.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("""
            select v from ProductVariant v
            join fetch v.product p
            join fetch p.seller
            join fetch p.category
            where v.id = :id
            """)
    Optional<ProductVariant> findWithProductById(@Param("id") Long id);

    @Query("""
            select v from ProductVariant v
            join fetch v.product p
            join fetch p.seller
            join fetch p.category
            where v.id in :ids
            """)
    List<ProductVariant> findWithProductByIdIn(@Param("ids") Collection<Long> ids);

    boolean existsBySku(String sku);
}

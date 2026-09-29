package com.zovira.catalog.repository;

import com.zovira.catalog.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("""
            select p from Product p
            join fetch p.seller
            join fetch p.category
            left join fetch p.brand
            where p.slug = :slug
            """)
    Optional<Product> findDetailBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);
}

package com.zovira.catalog.repository;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Storefront rail query. Sorting comes from the {@link Pageable}; returning a List skips the
     * count query that a Page would trigger.
     */
    @Query("""
            select p from Product p
            left join fetch p.brand
            where p.status = :status
              and p.totalStock >= :minStock
              and p.discountPercent >= :minDiscount
              and p.ratingCount >= :minRatings
              and (:onlyWith3d = false or p.modelUrl is not null)
            """)
    List<Product> findRail(@Param("status") ProductStatus status, @Param("minStock") int minStock,
            @Param("minDiscount") int minDiscount, @Param("minRatings") int minRatings,
            @Param("onlyWith3d") boolean onlyWith3d, Pageable pageable);

    @Query("select p from Product p left join fetch p.brand where p.id in :ids")
    List<Product> findSummariesByIdIn(@Param("ids") Collection<Long> ids);

    long countBySellerIdAndStatus(Long sellerId, ProductStatus status);

    /** Recomputes the denormalized stock total from active variants' inventory. */
    @Modifying
    @Query(value = """
            UPDATE products p
               SET total_stock = COALESCE((SELECT SUM(i.quantity_available)
                                             FROM inventory i
                                             JOIN product_variants v ON v.id = i.variant_id
                                            WHERE v.product_id = p.id AND v.active), 0)
             WHERE p.id IN (:ids)
            """, nativeQuery = true)
    int refreshTotalStock(@Param("ids") Collection<Long> productIds);
}

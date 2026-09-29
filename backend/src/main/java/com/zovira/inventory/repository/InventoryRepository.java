package com.zovira.inventory.repository;

import com.zovira.inventory.entity.Inventory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Stock mutations are single conditional UPDATE statements so concurrent checkouts can never
 * oversell: a reservation only succeeds if enough stock remains at the moment it executes.
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariantId(Long variantId);

    List<Inventory> findByVariantIdIn(Collection<Long> variantIds);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
               SET quantity_available = quantity_available - :qty,
                   quantity_reserved  = quantity_reserved + :qty,
                   version = version + 1, updated_at = now()
             WHERE variant_id = :variantId AND quantity_available >= :qty
            """, nativeQuery = true)
    int reserve(@Param("variantId") Long variantId, @Param("qty") int qty);

    /** Payment captured (or COD confirmed): reserved units become sold. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
               SET quantity_reserved = GREATEST(quantity_reserved - :qty, 0),
                   version = version + 1, updated_at = now()
             WHERE variant_id = :variantId
            """, nativeQuery = true)
    int commitReservation(@Param("variantId") Long variantId, @Param("qty") int qty);

    /** Unpaid order expired or cancelled before confirmation: return reserved units to sale. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
               SET quantity_available = quantity_available + LEAST(quantity_reserved, :qty),
                   quantity_reserved  = GREATEST(quantity_reserved - :qty, 0),
                   version = version + 1, updated_at = now()
             WHERE variant_id = :variantId
            """, nativeQuery = true)
    int releaseReservation(@Param("variantId") Long variantId, @Param("qty") int qty);

    /** Cancelled after confirmation or returned: put sold units back into stock. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE inventory
               SET quantity_available = quantity_available + :qty,
                   version = version + 1, updated_at = now()
             WHERE variant_id = :variantId
            """, nativeQuery = true)
    int restock(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Query(value = """
            SELECT COALESCE(SUM(i.quantity_available), 0)
              FROM inventory i JOIN product_variants v ON v.id = i.variant_id
             WHERE v.product_id = :productId AND v.active
            """, nativeQuery = true)
    int sumAvailableForProduct(@Param("productId") Long productId);
}

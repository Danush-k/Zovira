package com.zovira.catalog.repository;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;

public interface ProductRepositoryCustom {

    List<Product> findRail(ProductStatus status, int minStock, int minDiscount, int minRatings,
            boolean onlyWith3d, Pageable pageable);

    int adjustSoldCount(Long productId, int delta);

    int refreshTotalStock(Collection<Long> productIds);
}

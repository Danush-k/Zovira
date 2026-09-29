package com.zovira.catalog.service;

import com.zovira.catalog.dto.ProductDetailResponse;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.mapper.CatalogMapper;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.NotFoundException;
import com.zovira.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds and caches the static portion of a product page (content, variants, specs). Live stock
 * is merged on top per request by {@link ProductQueryService}.
 */
@Component
public class ProductDetailAssembler {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductDetailAssembler(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#slug")
    @Transactional(readOnly = true)
    public ProductDetailResponse load(String slug) {
        Product product = productRepository.findDetailBySlug(slug)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE || p.getStatus() == ProductStatus.INACTIVE)
                .orElseThrow(() -> NotFoundException.of("Product"));
        return CatalogMapper.toDetail(product, categoryService.path(product.getCategory().getId()));
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_DETAIL, key = "#slug")
    public void evict(String slug) {
        // eviction only
    }
}

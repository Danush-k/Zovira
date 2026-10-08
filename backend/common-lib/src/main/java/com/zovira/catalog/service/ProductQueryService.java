package com.zovira.catalog.service;

import com.zovira.catalog.dto.ProductDetailResponse;
import com.zovira.catalog.dto.ProductSummary;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.mapper.CatalogMapper;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.inventory.repository.InventoryRepository;
import com.zovira.settings.service.SettingsService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductDetailAssembler assembler;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SettingsService settingsService;
    private final ProductViewCounter viewCounter;

    public ProductQueryService(ProductDetailAssembler assembler, ProductRepository productRepository,
            InventoryRepository inventoryRepository, SettingsService settingsService,
            ProductViewCounter viewCounter) {
        this.assembler = assembler;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.settingsService = settingsService;
        this.viewCounter = viewCounter;
    }

    /** Product page: cached content with live per-variant stock. */
    @Transactional(readOnly = true)
    public ProductDetailResponse detail(String slug, boolean countView) {
        ProductDetailResponse cached = assembler.load(slug);
        if (countView) {
            viewCounter.increment(cached.id());
        }
        return withLiveStock(cached);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse withLiveStock(ProductDetailResponse detail) {
        List<Long> variantIds = detail.variants().stream().map(v -> v.id()).toList();
        return CatalogMapper.withStock(detail, CatalogMapper.byVariant(inventoryRepository.findByVariantIdIn(variantIds)),
                settingsService.current().maxQuantityPerItem());
    }

    /** Side-by-side comparison of up to four products. */
    @Transactional(readOnly = true)
    public List<ProductDetailResponse> compare(List<String> slugs) {
        if (slugs.isEmpty() || slugs.size() > 4) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Compare between 1 and 4 products");
        }
        return slugs.stream().distinct().map(s -> withLiveStock(assembler.load(s))).toList();
    }

    /** Summaries for the given ids in the same order, skipping any that are unavailable. */
    @Transactional(readOnly = true)
    public List<ProductSummary> summaries(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> byId = productRepository.findSummariesByIdIn(ids).stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(p -> p != null).map(CatalogMapper::toSummary).toList();
    }
}

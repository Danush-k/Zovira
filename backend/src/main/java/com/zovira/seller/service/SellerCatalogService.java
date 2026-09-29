package com.zovira.seller.service;

import com.zovira.catalog.entity.Brand;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductAttribute;
import com.zovira.catalog.entity.ProductImage;
import com.zovira.catalog.entity.ProductSpecification;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.catalog.repository.BrandRepository;
import com.zovira.catalog.repository.CategoryRepository;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.catalog.repository.ProductVariantRepository;
import com.zovira.catalog.service.ProductDetailAssembler;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.common.util.Slugs;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.config.CacheConfig;
import com.zovira.inventory.entity.Inventory;
import com.zovira.inventory.repository.InventoryRepository;
import com.zovira.seller.dto.InventoryRow;
import com.zovira.seller.dto.SellerProductRequest;
import com.zovira.seller.dto.SellerProductSummary;
import com.zovira.seller.dto.StockUpdateRequest;
import com.zovira.seller.entity.Seller;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A seller's own catalog. Products are always loaded with the seller id in the query, so one
 * seller can never read or edit another's listing by guessing an id.
 */
@Service
public class SellerCatalogService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final InventoryRepository inventory;
    private final ProductDetailAssembler assembler;
    private final Clock clock;

    public SellerCatalogService(ProductRepository products, ProductVariantRepository variants,
            CategoryRepository categories, BrandRepository brands, InventoryRepository inventory,
            ProductDetailAssembler assembler, Clock clock) {
        this.products = products;
        this.variants = variants;
        this.categories = categories;
        this.brands = brands;
        this.inventory = inventory;
        this.assembler = assembler;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerProductSummary> list(Long sellerId, String status, String query, Integer page,
            Integer size) {
        Specification<Product> spec = (root, q, cb) -> cb.equal(root.get("seller").get("id"), sellerId);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            ProductStatus parsed = ProductStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), like));
        }
        Page<Product> found = products.findAll(spec,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.of(found, SellerCatalogService::toSummary);
    }

    @Transactional(readOnly = true)
    public Product require(Long sellerId, Long productId) {
        return products.findById(productId)
                .filter(p -> p.getSeller().getId().equals(sellerId))
                .orElseThrow(() -> NotFoundException.of("Product"));
    }

    @CacheEvict(cacheNames = {CacheConfig.HOME, CacheConfig.BRANDS}, allEntries = true)
    @Transactional
    public SellerProductSummary create(Seller seller, SellerProductRequest request) {
        Category category = category(request.categoryId());
        Product product = new Product(seller, category, resolveBrand(request), request.title().trim(),
                Slugs.unique(request.title(), products::existsBySlug));
        apply(product, request, category);
        products.save(product);
        syncVariants(product, request);
        finish(product, request);
        return toSummary(product);
    }

    @CacheEvict(cacheNames = {CacheConfig.HOME, CacheConfig.BRANDS}, allEntries = true)
    @Transactional
    public SellerProductSummary update(Long sellerId, Long productId, SellerProductRequest request) {
        Product product = require(sellerId, productId);
        Category category = category(request.categoryId());
        product.setCategory(category);
        product.setBrand(resolveBrand(request));
        product.setTitle(request.title().trim());
        apply(product, request, category);
        syncVariants(product, request);
        finish(product, request);
        assembler.evict(product.getSlug());
        return toSummary(product);
    }

    @CacheEvict(cacheNames = {CacheConfig.HOME, CacheConfig.BRANDS}, allEntries = true)
    @Transactional
    public SellerProductSummary setStatus(Long sellerId, Long productId, ProductStatus status) {
        Product product = require(sellerId, productId);
        if (status == ProductStatus.ACTIVE) {
            requirePublishable(product);
            product.publish(clock.instant());
        } else {
            product.setStatus(status);
        }
        assembler.evict(product.getSlug());
        return toSummary(product);
    }

    @Transactional(readOnly = true)
    public List<InventoryRow> inventory(Long sellerId, boolean lowOnly) {
        return products.findAll((root, q, cb) -> cb.equal(root.get("seller").get("id"), sellerId),
                        Sort.by("title")).stream()
                .filter(p -> p.getStatus() != ProductStatus.ARCHIVED)
                .flatMap(p -> p.getVariants().stream().map(v -> row(p, v)))
                .filter(r -> !lowOnly || r.available() <= r.lowStockThreshold())
                .toList();
    }

    @Transactional
    public List<InventoryRow> updateStock(Long sellerId, StockUpdateRequest request) {
        Map<Long, StockUpdateRequest.Row> byVariant = request.rows().stream()
                .collect(Collectors.toMap(StockUpdateRequest.Row::variantId, Function.identity(), (a, b) -> b));
        List<ProductVariant> found = variants.findWithProductByIdIn(byVariant.keySet());
        Set<Long> productIds = new HashSet<>();
        for (ProductVariant variant : found) {
            if (!variant.getProduct().getSeller().getId().equals(sellerId)) {
                throw NotFoundException.of("Product option");
            }
            StockUpdateRequest.Row row = byVariant.get(variant.getId());
            Inventory record = inventory.findByVariantId(variant.getId())
                    .orElseGet(() -> inventory.save(new Inventory(variant, 0)));
            record.setQuantityAvailable(row.available());
            if (row.lowStockThreshold() != null) {
                record.setLowStockThreshold(row.lowStockThreshold());
            }
            productIds.add(variant.getProduct().getId());
        }
        inventory.flush();
        products.refreshTotalStock(productIds);
        productIds.forEach(id -> products.findById(id).ifPresent(p -> assembler.evict(p.getSlug())));
        return inventory(sellerId, false);
    }

    // ---------------------------------------------------------------------------------------

    private void apply(Product product, SellerProductRequest r, Category category) {
        product.setShortDescription(trim(r.shortDescription()));
        product.setDescription(trim(r.description()));
        product.setHighlights(r.highlights() == null ? List.of() : r.highlights());
        product.setTags(trim(r.tags()));
        product.setWarranty(trim(r.warranty()));
        product.setReturnable(r.returnable());
        product.setReturnWindowDays(r.returnable() ? Math.max(1, r.returnWindowDays()) : 0);
        product.setCodAvailable(r.codAvailable());
        product.setModelUrl(trim(r.modelUrl()));
        product.setArModelUrl(trim(r.arModelUrl()));
        product.setSearchKeywords(keywords(product, category, r));

        product.getAttributes().clear();
        int position = 0;
        for (SellerProductRequest.AttributeInput a : nullToEmpty(r.attributes())) {
            product.addAttribute(new ProductAttribute(a.name().trim(), position++, a.options()));
        }
        product.getSpecifications().clear();
        position = 0;
        for (SellerProductRequest.SpecInput s : nullToEmpty(r.specifications())) {
            product.addSpecification(new ProductSpecification(
                    s.group() == null || s.group().isBlank() ? "General" : s.group().trim(), s.name().trim(),
                    s.value().trim(), position++));
        }
    }

    /**
     * Reconciles submitted variants with stored ones: existing variants are updated in place so
     * their inventory and order history survive, new ones are created with stock, and omitted ones
     * are deactivated rather than deleted (they may appear in past orders).
     */
    private void syncVariants(Product product, SellerProductRequest request) {
        Map<Long, ProductVariant> existing = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> submitted = new HashSet<>();
        int position = 0;
        boolean anyDefault = false;

        for (SellerProductRequest.VariantInput input : request.variants()) {
            if (input.mrp().compareTo(input.price()) < 0) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                        "MRP can't be lower than the selling price for " + variantName(input));
            }
            ProductVariant variant = input.id() == null ? null : existing.get(input.id());
            if (variant == null) {
                String sku = input.sku() == null || input.sku().isBlank()
                        ? generateSku(product, position) : input.sku().trim().toUpperCase(Locale.ROOT);
                if (variants.existsBySku(sku)) {
                    throw new BusinessException(ErrorCodes.CONFLICT, "SKU " + sku + " is already in use");
                }
                variant = new ProductVariant(sku, variantName(input), input.price(), input.mrp());
                product.addVariant(variant);
                variants.flush();
                inventory.save(new Inventory(variant, input.stock()));
            } else {
                variant.setName(variantName(input));
                variant.setPrice(input.price());
                variant.setMrp(input.mrp());
                Inventory record = inventory.findByVariantId(variant.getId())
                        .orElseGet(() -> inventory.save(new Inventory(existing.get(input.id()), 0)));
                record.setQuantityAvailable(input.stock());
                submitted.add(variant.getId());
            }
            variant.setOptions(input.options() == null ? Map.of() : input.options());
            variant.setWeightGrams(input.weightGrams());
            variant.setActive(input.active());
            variant.setPosition(position++);
            variant.setDefaultVariant(input.isDefault() && input.active());
            anyDefault |= variant.isDefaultVariant();
        }

        existing.values().stream().filter(v -> !submitted.contains(v.getId())).forEach(v -> {
            v.setActive(false);
            v.setDefaultVariant(false);
        });
        if (!anyDefault) {
            product.getVariants().stream().filter(ProductVariant::isActive).findFirst()
                    .ifPresent(v -> v.setDefaultVariant(true));
        }

        product.getImages().clear();
        position = 0;
        for (SellerProductRequest.ImageInput image : nullToEmpty(request.images())) {
            ProductImage productImage = new ProductImage(image.url(), trim(image.altText()), position++);
            if (image.variantOption() != null) {
                product.getVariants().stream()
                        .filter(v -> v.getOptions().containsValue(image.variantOption()))
                        .findFirst()
                        .ifPresent(productImage::setVariant);
            }
            product.addImage(productImage);
        }
    }

    private void finish(Product product, SellerProductRequest request) {
        product.refreshPricing();
        variants.flush();
        inventory.flush();
        products.flush();
        products.refreshTotalStock(List.of(product.getId()));
        if (request.publish()) {
            requirePublishable(product);
            product.publish(clock.instant());
        } else if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.INACTIVE);
        }
    }

    private static void requirePublishable(Product product) {
        if (product.getImages().isEmpty()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Add at least one image before publishing");
        }
        if (product.getVariants().stream().noneMatch(ProductVariant::isActive)) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Add at least one active variant before publishing");
        }
        if (product.getShortDescription() == null || product.getShortDescription().isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Add a short description before publishing");
        }
    }

    private Category category(Long id) {
        Category category = categories.findById(id).orElseThrow(() -> NotFoundException.of("Category"));
        if (!category.isActive()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "That category isn't available");
        }
        return category;
    }

    /** Uses an existing brand, or creates one when the seller types a new name. */
    private Brand resolveBrand(SellerProductRequest request) {
        if (request.brandId() != null) {
            return brands.findById(request.brandId()).orElseThrow(() -> NotFoundException.of("Brand"));
        }
        String name = request.newBrandName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return brands.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> brands.save(new Brand(name.trim(), Slugs.unique(name, s -> brands.findBySlug(s).isPresent()))));
    }

    private String generateSku(Product product, int index) {
        String base = "ZV-" + product.getId() + "-" + (index + 1);
        String candidate = base;
        int suffix = 2;
        while (variants.existsBySku(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private static String variantName(SellerProductRequest.VariantInput input) {
        if (input.name() != null && !input.name().isBlank()) {
            return input.name().trim();
        }
        if (input.options() == null || input.options().isEmpty()) {
            return "Standard";
        }
        return String.join(", ", input.options().values());
    }

    private static String keywords(Product product, Category category, SellerProductRequest request) {
        List<String> parts = new ArrayList<>();
        Optional.ofNullable(product.getBrand()).map(Brand::getName).ifPresent(parts::add);
        parts.add(category.getName());
        if (category.getParent() != null) {
            parts.add(category.getParent().getName());
        }
        if (request.tags() != null) {
            parts.add(request.tags().replace(',', ' '));
        }
        return String.join(" ", parts);
    }

    private InventoryRow row(Product p, ProductVariant v) {
        Inventory record = inventory.findByVariantId(v.getId()).orElse(null);
        return new InventoryRow(v.getId(), p.getId(), p.getTitle(), p.getSlug(), p.primaryImageUrl(), v.getName(),
                v.getOptions(), v.getSku(), v.getPrice(), record == null ? 0 : record.getQuantityAvailable(),
                record == null ? 0 : record.getQuantityReserved(), record == null ? 5 : record.getLowStockThreshold(),
                v.isActive());
    }

    static SellerProductSummary toSummary(Product p) {
        return new SellerProductSummary(p.getId(), p.getSlug(), p.getTitle(), p.primaryImageUrl(),
                p.getCategory().getName(), p.getBrand() == null ? null : p.getBrand().getName(), p.getStatus().name(),
                p.getMinPrice(), p.getMinMrp(), p.getTotalStock(),
                (int) p.getVariants().stream().filter(ProductVariant::isActive).count(),
                p.getTotalStock() > 0 && p.getTotalStock() <= 5, p.getRatingAverage(), p.getRatingCount(),
                p.getSoldCount(), p.getUpdatedAt());
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

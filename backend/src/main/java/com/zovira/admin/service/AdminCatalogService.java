package com.zovira.admin.service;

import com.zovira.admin.dto.CategoryRequest;
import com.zovira.audit.service.AuditService;
import com.zovira.catalog.dto.CategoryNode;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.repository.CategoryRepository;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.catalog.service.CategoryService;
import com.zovira.catalog.service.ProductDetailAssembler;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.common.util.Slugs;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.config.CacheConfig;
import com.zovira.seller.dto.SellerProductSummary;
import com.zovira.seller.service.SellerCatalogService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Platform-wide catalog moderation: categories, and any seller's listing. */
@Service
public class AdminCatalogService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CategoryService categoryService;
    private final ProductDetailAssembler assembler;
    private final AuditService audit;

    public AdminCatalogService(CategoryRepository categories, ProductRepository products,
            CategoryService categoryService, ProductDetailAssembler assembler, AuditService audit) {
        this.categories = categories;
        this.products = products;
        this.categoryService = categoryService;
        this.assembler = assembler;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CategoryNode> tree() {
        return categoryService.tree();
    }

    @CacheEvict(cacheNames = {CacheConfig.CATEGORY_TREE, CacheConfig.HOME}, allEntries = true)
    @Transactional
    public CategoryNode create(CategoryRequest request) {
        Category category = new Category(request.name().trim(), Slugs.unique(request.name(),
                slug -> categories.findBySlug(slug).isPresent()));
        apply(category, request);
        categories.save(category);
        audit.record("CATEGORY_CREATED", "CATEGORY", category.getId(), Map.of("name", category.getName()));
        return toNode(category);
    }

    @CacheEvict(cacheNames = {CacheConfig.CATEGORY_TREE, CacheConfig.HOME}, allEntries = true)
    @Transactional
    public CategoryNode update(Long id, CategoryRequest request) {
        Category category = categories.findById(id).orElseThrow(() -> NotFoundException.of("Category"));
        category.setName(request.name().trim());
        apply(category, request);
        audit.record("CATEGORY_UPDATED", "CATEGORY", id, Map.of("name", category.getName()));
        return toNode(category);
    }

    private void apply(Category category, CategoryRequest request) {
        if (request.parentId() != null) {
            Category parent = categories.findById(request.parentId())
                    .orElseThrow(() -> NotFoundException.of("Parent category"));
            if (category.getId() != null && wouldCycle(parent, category.getId())) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "A category can't be nested inside itself");
            }
            if (parent.getParent() != null) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Categories can only be nested two levels deep");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        category.setDescription(blankToNull(request.description()));
        category.setIcon(blankToNull(request.icon()));
        category.setImageUrl(blankToNull(request.imageUrl()));
        category.setTaxRate(request.taxRate());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active());
    }

    private static boolean wouldCycle(Category parent, Long id) {
        for (Category c = parent; c != null; c = c.getParent()) {
            if (id.equals(c.getId())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerProductSummary> listProducts(String status, String query, Long sellerId, Integer page,
            Integer size) {
        Specification<Product> spec = Specification.allOf();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            ProductStatus parsed = ProductStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (sellerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("seller").get("id"), sellerId));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), like));
        }
        Page<Product> found = products.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.of(found, (Product p) -> SellerCatalogService.toSummary(p));
    }

    /** Takes a listing down (or restores it) regardless of which seller owns it. */
    @CacheEvict(cacheNames = {CacheConfig.HOME}, allEntries = true)
    @Transactional
    public SellerProductSummary setProductStatus(Long productId, ProductStatus status, String reason) {
        Product product = products.findById(productId).orElseThrow(() -> NotFoundException.of("Product"));
        product.setStatus(status);
        assembler.evict(product.getSlug());
        audit.record("PRODUCT_STATUS_CHANGED", "PRODUCT", productId,
                Map.of("status", status.name(), "reason", String.valueOf(reason)));
        return SellerCatalogService.toSummary(product);
    }

    private static CategoryNode toNode(Category c) {
        return new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getIcon(), c.getImageUrl(), c.getDescription(),
                List.of());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

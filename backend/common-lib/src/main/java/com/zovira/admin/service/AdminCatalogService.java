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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Platform-wide catalog moderation: categories, and any seller's listing. */
@Service
public class AdminCatalogService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CategoryService categoryService;
    private final ProductDetailAssembler assembler;
    private final MongoTemplate mongoTemplate;
    private final AuditService audit;

    public AdminCatalogService(CategoryRepository categories, ProductRepository products,
            CategoryService categoryService, ProductDetailAssembler assembler,
            MongoTemplate mongoTemplate, AuditService audit) {
        this.categories = categories;
        this.products = products;
        this.categoryService = categoryService;
        this.assembler = assembler;
        this.mongoTemplate = mongoTemplate;
        this.audit = audit;
    }

    public List<CategoryNode> tree() {
        return categoryService.tree();
    }

    public CategoryNode create(CategoryRequest request) {
        return createCategory(request);
    }

    public CategoryNode update(Long id, CategoryRequest request) {
        return updateCategory(id, request);
    }

    @CacheEvict(cacheNames = {CacheConfig.HOME, CacheConfig.CATEGORY_TREE}, allEntries = true)
    public CategoryNode createCategory(CategoryRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = categories.findById(request.parentId())
                    .orElseThrow(() -> NotFoundException.of("Parent category"));
        }
        String slug = Slugs.slugify(request.name());
        Category category = new Category(request.name(), slug);
        category.setParent(parent);
        category.setDescription(blankToNull(request.description()));
        category.setImageUrl(blankToNull(request.imageUrl()));
        category.setIcon(blankToNull(request.icon()));
        if (request.taxRate() != null) {
            category.setTaxRate(request.taxRate());
        }
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active());
        Category saved = categories.save(category);
        audit.record("CATEGORY_CREATED", "CATEGORY", saved.getId(), Map.of("name", saved.getName()));
        return toNode(saved);
    }

    @CacheEvict(cacheNames = {CacheConfig.HOME, CacheConfig.CATEGORY_TREE}, allEntries = true)
    public CategoryNode updateCategory(Long id, CategoryRequest request) {
        Category category = categories.findById(id).orElseThrow(() -> NotFoundException.of("Category"));
        if (request.parentId() != null) {
            if (id.equals(request.parentId())) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST, "A category cannot be its own parent");
            }
            Category parent = categories.findById(request.parentId())
                    .orElseThrow(() -> NotFoundException.of("Parent category"));
            if (isDescendant(id, parent)) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST, "A category cannot be a child of its own descendant");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        category.setName(request.name());
        category.setSlug(Slugs.slugify(request.name()));
        category.setDescription(blankToNull(request.description()));
        category.setImageUrl(blankToNull(request.imageUrl()));
        category.setIcon(blankToNull(request.icon()));
        if (request.taxRate() != null) {
            category.setTaxRate(request.taxRate());
        }
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active());
        Category saved = categories.save(category);
        audit.record("CATEGORY_UPDATED", "CATEGORY", saved.getId(), Map.of("name", saved.getName()));
        return toNode(saved);
    }

    private boolean isDescendant(Long id, Category parent) {
        for (Category c = parent; c != null; c = c.getParent()) {
            if (id.equals(c.getId())) {
                return true;
            }
        }
        return false;
    }

    public PageResponse<SellerProductSummary> listProducts(String status, String query, Long sellerId, Integer page,
            Integer size) {
        Criteria criteria = new Criteria();
        List<Criteria> andList = new ArrayList<>();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            ProductStatus parsed = ProductStatus.valueOf(status.toUpperCase(Locale.ROOT));
            andList.add(Criteria.where("status").is(parsed));
        }
        if (sellerId != null) {
            andList.add(Criteria.where("sellerId").is(sellerId));
        }
        if (query != null && !query.isBlank()) {
            andList.add(Criteria.where("title").regex(query.trim(), "i"));
        }
        if (!andList.isEmpty()) {
            criteria.andOperator(andList.toArray(new Criteria[0]));
        }
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        long total = mongoTemplate.count(new Query(criteria), Product.class);
        List<Product> list = mongoTemplate.find(new Query(criteria).with(pageable), Product.class);
        Page<Product> found = new PageImpl<>(list, pageable, total);
        return PageResponse.of(found, SellerCatalogService::toSummary);
    }

    /** Takes a listing down (or restores it) regardless of which seller owns it. */
    @CacheEvict(cacheNames = {CacheConfig.HOME}, allEntries = true)
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

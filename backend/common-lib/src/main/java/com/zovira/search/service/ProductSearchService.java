package com.zovira.search.service;

import com.zovira.catalog.dto.ProductSummary;
import com.zovira.catalog.entity.Brand;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.mapper.CatalogMapper;
import com.zovira.catalog.service.CategoryService;
import com.zovira.common.web.PageResponse;
import com.zovira.search.dto.SearchCriteria;
import com.zovira.search.dto.SearchResponse;
import com.zovira.search.dto.SearchResponse.CountBucket;
import com.zovira.search.dto.SearchResponse.FacetValue;
import com.zovira.search.dto.SearchSort;
import com.zovira.search.dto.SuggestResponse;
import com.zovira.search.entity.SearchHistory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Product search on MongoDB.
 */
@Service
public class ProductSearchService {

    private final MongoTemplate mongoTemplate;
    private final CategoryService categoryService;

    public ProductSearchService(MongoTemplate mongoTemplate, CategoryService categoryService) {
        this.mongoTemplate = mongoTemplate;
        this.categoryService = categoryService;
    }

    public SearchResponse search(SearchCriteria c) {
        Query scopeQuery = buildScopeQuery(c);
        List<Product> scopeProducts = mongoTemplate.find(scopeQuery, Product.class);

        Query refinedQuery = buildRefinedQuery(c);
        long total = mongoTemplate.count(refinedQuery, Product.class);

        Sort sort = toSort(c.sort());
        PageRequest pageRequest = PageRequest.of(c.page(), c.size(), sort);
        refinedQuery.with(pageRequest);

        List<Product> content = mongoTemplate.find(refinedQuery, Product.class);
        List<ProductSummary> summaries = content.stream().map(CatalogMapper::toSummary).toList();

        SearchResponse.Facets facets = buildFacets(scopeProducts);
        String sortName = c.sort() != null ? c.sort().name() : SearchSort.POPULAR.name();

        return new SearchResponse(PageResponse.of(summaries, c.page(), c.size(), total), facets, sortName);
    }

    private Query buildScopeQuery(SearchCriteria c) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("status").is(ProductStatus.ACTIVE));

        if (c.hasQuery()) {
            String q = c.query().trim();
            Pattern p = Pattern.compile(".*" + Pattern.quote(q) + ".*", Pattern.CASE_INSENSITIVE);
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(p),
                    Criteria.where("searchKeywords").regex(p),
                    Criteria.where("tags").regex(p)));
        }

        if (c.category() != null && !c.category().isBlank()) {
            java.util.Set<Long> categoryIds = categoryService.selfAndDescendantIds(c.category());
            criteriaList.add(Criteria.where("categoryId").in(categoryIds));
        }

        if (c.seller() != null && !c.seller().isBlank()) {
            criteriaList.add(Criteria.where("seller.slug").is(c.seller()));
        }

        if (c.only3d()) {
            criteriaList.add(Criteria.where("modelUrl").ne(null).ne(""));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return query;
    }

    private Query buildRefinedQuery(SearchCriteria c) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("status").is(ProductStatus.ACTIVE));

        if (c.hasQuery()) {
            String q = c.query().trim();
            Pattern p = Pattern.compile(".*" + Pattern.quote(q) + ".*", Pattern.CASE_INSENSITIVE);
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(p),
                    Criteria.where("searchKeywords").regex(p),
                    Criteria.where("tags").regex(p)));
        }

        if (c.category() != null && !c.category().isBlank()) {
            java.util.Set<Long> categoryIds = categoryService.selfAndDescendantIds(c.category());
            criteriaList.add(Criteria.where("categoryId").in(categoryIds));
        }

        if (c.seller() != null && !c.seller().isBlank()) {
            criteriaList.add(Criteria.where("seller.slug").is(c.seller()));
        }

        if (c.only3d()) {
            criteriaList.add(Criteria.where("modelUrl").ne(null).ne(""));
        }

        if (c.brands() != null && !c.brands().isEmpty()) {
            criteriaList.add(Criteria.where("brand.slug").in(c.brands()));
        }

        if (c.minPrice() != null) {
            criteriaList.add(Criteria.where("minPrice").gte(c.minPrice()));
        }

        if (c.maxPrice() != null) {
            criteriaList.add(Criteria.where("minPrice").lte(c.maxPrice()));
        }

        if (c.minRating() != null) {
            criteriaList.add(Criteria.where("ratingAverage").gte(BigDecimal.valueOf(c.minRating())));
        }

        if (c.minDiscount() != null) {
            criteriaList.add(Criteria.where("discountPercent").gte(c.minDiscount()));
        }

        if (c.inStockOnly()) {
            criteriaList.add(Criteria.where("totalStock").gt(0));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return query;
    }

    private SearchResponse.Facets buildFacets(List<Product> products) {
        Map<String, FacetValue> categoryMap = new LinkedHashMap<>();
        Map<String, FacetValue> brandMap = new LinkedHashMap<>();
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        long r4 = 0;
        long r3 = 0;
        long d10 = 0;
        long d25 = 0;
        long d40 = 0;
        long inStock = 0;
        long with3d = 0;

        for (Product p : products) {
            if (p.getCategory() != null) {
                String slug = p.getCategory().getSlug();
                String name = p.getCategory().getName();
                categoryMap.compute(slug, (k, v) -> v == null ? new FacetValue(slug, name, 1) : new FacetValue(slug, name, v.count() + 1));
            }
            if (p.getBrand() != null) {
                String slug = p.getBrand().getSlug();
                String name = p.getBrand().getName();
                brandMap.compute(slug, (k, v) -> v == null ? new FacetValue(slug, name, 1) : new FacetValue(slug, name, v.count() + 1));
            }
            if (p.getMinPrice() != null) {
                if (minPrice == null || p.getMinPrice().compareTo(minPrice) < 0) minPrice = p.getMinPrice();
                if (maxPrice == null || p.getMinPrice().compareTo(maxPrice) > 0) maxPrice = p.getMinPrice();
            }
            if (p.getRatingAverage() != null) {
                if (p.getRatingAverage().compareTo(BigDecimal.valueOf(4)) >= 0) r4++;
                if (p.getRatingAverage().compareTo(BigDecimal.valueOf(3)) >= 0) r3++;
            }
            if (p.getDiscountPercent() >= 10) d10++;
            if (p.getDiscountPercent() >= 25) d25++;
            if (p.getDiscountPercent() >= 40) d40++;
            if (p.getTotalStock() > 0) inStock++;
            if (p.getModelUrl() != null && !p.getModelUrl().isBlank()) with3d++;
        }

        List<FacetValue> categoryList = categoryMap.values().stream()
                .sorted(Comparator.comparingLong(FacetValue::count).reversed())
                .limit(20)
                .toList();
        List<FacetValue> brandList = brandMap.values().stream()
                .sorted(Comparator.comparingLong(FacetValue::count).reversed())
                .limit(40)
                .toList();

        return new SearchResponse.Facets(
                categoryList,
                brandList,
                minPrice,
                maxPrice,
                List.of(new CountBucket(4, r4), new CountBucket(3, r3)),
                List.of(new CountBucket(10, d10), new CountBucket(25, d25), new CountBucket(40, d40)),
                inStock,
                with3d
        );
    }

    public SuggestResponse suggest(String raw) {
        String q = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return new SuggestResponse(List.of(), List.of(), List.of(), List.of());
        }

        Pattern p = Pattern.compile(".*" + Pattern.quote(q) + ".*", Pattern.CASE_INSENSITIVE);

        // Products
        Query prodQuery = new Query(new Criteria().andOperator(
                Criteria.where("status").is(ProductStatus.ACTIVE),
                new Criteria().orOperator(
                        Criteria.where("title").regex(p),
                        Criteria.where("searchKeywords").regex(p)
                )
        )).with(Sort.by(Sort.Direction.DESC, "soldCount")).limit(6);

        List<SuggestResponse.ProductHit> products = mongoTemplate.find(prodQuery, Product.class).stream()
                .map(prod -> new SuggestResponse.ProductHit(prod.getSlug(), prod.getTitle(), prod.primaryImageUrl(), prod.getMinPrice()))
                .toList();

        // Categories
        Query catQuery = new Query(Criteria.where("active").is(true).and("name").regex(p)).limit(3);
        List<SuggestResponse.Link> categories = mongoTemplate.find(catQuery, Category.class).stream()
                .map(cat -> new SuggestResponse.Link(cat.getSlug(), cat.getName()))
                .toList();

        // Brands
        Query brandQuery = new Query(Criteria.where("name").regex(p)).limit(3);
        List<SuggestResponse.Link> brands = mongoTemplate.find(brandQuery, Brand.class).stream()
                .map(b -> new SuggestResponse.Link(b.getSlug(), b.getName()))
                .toList();

        // Search history
        Query histQuery = new Query(Criteria.where("query").regex(Pattern.compile("^" + Pattern.quote(q), Pattern.CASE_INSENSITIVE))
                .and("resultCount").gt(0)
                .and("createdAt").gt(Instant.now().minus(Duration.ofDays(30))))
                .limit(20);
        List<String> queries = mongoTemplate.find(histQuery, SearchHistory.class).stream()
                .map(sh -> sh.getQuery().toLowerCase(Locale.ROOT))
                .distinct()
                .limit(4)
                .toList();

        return new SuggestResponse(products, categories, brands, queries);
    }

    public List<String> trending() {
        Query histQuery = new Query(Criteria.where("resultCount").gt(0)
                .and("createdAt").gt(Instant.now().minus(Duration.ofDays(7))))
                .limit(40);
        return mongoTemplate.find(histQuery, SearchHistory.class).stream()
                .map(sh -> sh.getQuery().toLowerCase(Locale.ROOT))
                .distinct()
                .limit(8)
                .toList();
    }

    private Sort toSort(SearchSort sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return switch (sort) {
            case RELEVANCE, POPULAR -> Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "id"));
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "minPrice").and(Sort.by(Sort.Direction.DESC, "id"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "minPrice").and(Sort.by(Sort.Direction.DESC, "id"));
            case NEWEST -> Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id"));
            case RATING -> Sort.by(Sort.Direction.DESC, "ratingAverage").and(Sort.by(Sort.Direction.DESC, "ratingCount")).and(Sort.by(Sort.Direction.DESC, "id"));
            case DISCOUNT -> Sort.by(Sort.Direction.DESC, "discountPercent").and(Sort.by(Sort.Direction.DESC, "soldCount")).and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }
}

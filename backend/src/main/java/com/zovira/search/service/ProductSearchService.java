package com.zovira.search.service;

import com.zovira.catalog.dto.ProductSummary;
import com.zovira.catalog.service.CategoryService;
import com.zovira.common.web.PageResponse;
import com.zovira.search.dto.SearchCriteria;
import com.zovira.search.dto.SearchResponse;
import com.zovira.search.dto.SearchResponse.CountBucket;
import com.zovira.search.dto.SearchResponse.FacetValue;
import com.zovira.search.dto.SuggestResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product search on PostgreSQL.
 *
 * <p>Matching combines full-text search with prefix terms ({@code "iph 16"} finds "iPhone 16") and
 * trigram word similarity for typos ({@code "iphne"} still finds "iPhone"). Relevance blends text
 * rank, title similarity and popularity. Facet counts are computed over the query and category
 * scope, ignoring the refining filters, so shoppers can see what other choices would yield.
 *
 * <p>The service sits behind a narrow interface (criteria in, page + facets out) so it can be
 * swapped for OpenSearch without touching controllers or the frontend.
 */
@Service
public class ProductSearchService {

    private static final double TITLE_SIMILARITY = 0.4;
    private static final double KEYWORD_SIMILARITY = 0.55;

    private static final String SUMMARY_COLUMNS = """
            p.id, p.slug, p.title, b.name AS brand_name, p.min_price, p.min_mrp, p.discount_percent,
            p.rating_average, p.rating_count, p.total_stock, p.model_url,
            (SELECT pi.url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.position, pi.id LIMIT 1) AS image_url,
            (SELECT v.id FROM product_variants v WHERE v.product_id = p.id AND v.active
              ORDER BY v.is_default DESC, v.price, v.id LIMIT 1) AS default_variant_id,
            (SELECT count(*) FROM product_variants v WHERE v.product_id = p.id AND v.active) AS variant_count
            """;

    private static final String FROM = """
            FROM products p
            LEFT JOIN brands b ON b.id = p.brand_id
            JOIN sellers s ON s.id = p.seller_id
            JOIN categories c ON c.id = p.category_id
            """;

    private static final RowMapper<ProductSummary> SUMMARY = (rs, i) -> new ProductSummary(rs.getLong("id"),
            rs.getString("slug"), rs.getString("title"), rs.getString("brand_name"), rs.getString("image_url"),
            rs.getBigDecimal("min_price"), rs.getBigDecimal("min_mrp"), rs.getInt("discount_percent"),
            rs.getBigDecimal("rating_average"), rs.getInt("rating_count"), rs.getInt("total_stock") > 0,
            rs.getString("model_url") != null, (Long) rs.getObject("default_variant_id"), rs.getInt("variant_count"));

    private final NamedParameterJdbcTemplate jdbc;
    private final CategoryService categoryService;

    public ProductSearchService(NamedParameterJdbcTemplate jdbc, CategoryService categoryService) {
        this.jdbc = jdbc;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(SearchCriteria c) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String tsQuery = toTsQuery(c.query());
        boolean text = tsQuery != null;
        String scope = scopeWhere(c, tsQuery, params);
        String refined = scope + refineWhere(c, params);

        String rank = text
                ? "(ts_rank_cd(p.search_vector, to_tsquery('simple', :tsq)) * 2 + word_similarity(:q, lower(p.title))"
                        + " + ln(p.sold_count + 1) * 0.02)"
                : "0";
        String orderBy = c.sort().orderBy();

        params.addValue("limit", c.size()).addValue("offset", (long) c.page() * c.size());
        List<ProductSummary> content = jdbc.query("SELECT " + SUMMARY_COLUMNS + ", " + rank + " AS rank " + FROM
                + "WHERE " + refined + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset", params, SUMMARY);
        Long total = jdbc.queryForObject("SELECT count(*) " + FROM + "WHERE " + refined, params, Long.class);

        return new SearchResponse(PageResponse.of(content, c.page(), c.size(), total == null ? 0 : total),
                facets(scope, c, params), c.sort().name());
    }

    private SearchResponse.Facets facets(String scope, SearchCriteria c, MapSqlParameterSource params) {
        List<FacetValue> brands = jdbc.query("SELECT b.slug, b.name, count(*) AS n " + FROM + "WHERE " + scope
                + " AND b.id IS NOT NULL GROUP BY b.slug, b.name ORDER BY n DESC, b.name LIMIT 40", params,
                (rs, i) -> new FacetValue(rs.getString("slug"), rs.getString("name"), rs.getLong("n")));
        List<FacetValue> categories = jdbc.query("SELECT c.slug, c.name, count(*) AS n " + FROM + "WHERE " + scope
                + " GROUP BY c.slug, c.name ORDER BY n DESC, c.name LIMIT 20", params,
                (rs, i) -> new FacetValue(rs.getString("slug"), rs.getString("name"), rs.getLong("n")));
        Map<String, Object> agg = jdbc.queryForMap("""
                SELECT min(p.min_price) AS min_price, max(p.min_price) AS max_price,
                       count(*) FILTER (WHERE p.rating_average >= 4) AS r4,
                       count(*) FILTER (WHERE p.rating_average >= 3) AS r3,
                       count(*) FILTER (WHERE p.discount_percent >= 10) AS d10,
                       count(*) FILTER (WHERE p.discount_percent >= 25) AS d25,
                       count(*) FILTER (WHERE p.discount_percent >= 40) AS d40,
                       count(*) FILTER (WHERE p.total_stock > 0) AS in_stock,
                       count(*) FILTER (WHERE p.model_url IS NOT NULL) AS with3d
                """ + FROM + "WHERE " + scope, params);
        return new SearchResponse.Facets(categories, brands, (BigDecimal) agg.get("min_price"),
                (BigDecimal) agg.get("max_price"),
                List.of(new CountBucket(4, num(agg, "r4")), new CountBucket(3, num(agg, "r3"))),
                List.of(new CountBucket(10, num(agg, "d10")), new CountBucket(25, num(agg, "d25")),
                        new CountBucket(40, num(agg, "d40"))),
                num(agg, "in_stock"), num(agg, "with3d"));
    }

    /** Autocomplete: best product matches plus matching categories, brands and popular searches. */
    @Transactional(readOnly = true)
    public SuggestResponse suggest(String raw) {
        String q = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        String tsQuery = toTsQuery(q);
        if (tsQuery == null) {
            return new SuggestResponse(List.of(), List.of(), List.of(), List.of());
        }
        MapSqlParameterSource params = new MapSqlParameterSource("q", q).addValue("tsq", tsQuery)
                .addValue("like", escapeLike(q) + "%");
        List<SuggestResponse.ProductHit> products = jdbc.query("""
                SELECT p.slug, p.title, p.min_price,
                       (SELECT pi.url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.position LIMIT 1) AS image_url
                  FROM products p
                 WHERE p.status = 'ACTIVE'
                   AND (p.search_vector @@ to_tsquery('simple', :tsq) OR word_similarity(:q, lower(p.title)) >= 0.4)
                 ORDER BY ts_rank_cd(p.search_vector, to_tsquery('simple', :tsq)) * 2
                          + word_similarity(:q, lower(p.title)) + ln(p.sold_count + 1) * 0.02 DESC
                 LIMIT 6
                """, params, (rs, i) -> new SuggestResponse.ProductHit(rs.getString("slug"), rs.getString("title"),
                rs.getString("image_url"), rs.getBigDecimal("min_price")));
        List<SuggestResponse.Link> categories = jdbc.query("""
                SELECT slug, name FROM categories
                 WHERE active AND (lower(name) LIKE :like OR word_similarity(:q, lower(name)) >= 0.5)
                 ORDER BY word_similarity(:q, lower(name)) DESC LIMIT 3
                """, params, (rs, i) -> new SuggestResponse.Link(rs.getString("slug"), rs.getString("name")));
        List<SuggestResponse.Link> brands = jdbc.query("""
                SELECT slug, name FROM brands
                 WHERE lower(name) LIKE :like OR word_similarity(:q, lower(name)) >= 0.5
                 ORDER BY word_similarity(:q, lower(name)) DESC LIMIT 3
                """, params, (rs, i) -> new SuggestResponse.Link(rs.getString("slug"), rs.getString("name")));
        List<String> queries = jdbc.queryForList("""
                SELECT lower(query) AS q FROM search_history
                 WHERE lower(query) LIKE :like AND result_count > 0 AND created_at > now() - interval '30 days'
                 GROUP BY lower(query) ORDER BY count(*) DESC LIMIT 4
                """, params, String.class);
        return new SuggestResponse(products, categories, brands, queries);
    }

    /** Most searched terms over the last week, for the empty search box. */
    @Transactional(readOnly = true)
    public List<String> trending() {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT lower(query) FROM search_history
                 WHERE created_at > now() - interval '7 days' AND result_count > 0
                 GROUP BY lower(query) ORDER BY count(*) DESC LIMIT 8
                """, String.class);
    }

    private String scopeWhere(SearchCriteria c, String tsQuery, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder("p.status = 'ACTIVE'");
        if (tsQuery != null) {
            params.addValue("tsq", tsQuery).addValue("q", c.query().trim().toLowerCase(Locale.ROOT));
            where.append(" AND (p.search_vector @@ to_tsquery('simple', :tsq)")
                    .append(" OR word_similarity(:q, lower(p.title)) >= ").append(TITLE_SIMILARITY)
                    .append(" OR word_similarity(:q, lower(coalesce(p.search_keywords, ''))) >= ")
                    .append(KEYWORD_SIMILARITY).append(')');
        }
        if (c.category() != null && !c.category().isBlank()) {
            params.addValue("categoryIds", categoryService.selfAndDescendantIds(c.category()));
            where.append(" AND p.category_id IN (:categoryIds)");
        }
        if (c.seller() != null && !c.seller().isBlank()) {
            params.addValue("seller", c.seller());
            where.append(" AND s.slug = :seller");
        }
        if (c.only3d()) {
            where.append(" AND p.model_url IS NOT NULL");
        }
        return where.toString();
    }

    private static String refineWhere(SearchCriteria c, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder();
        if (c.brands() != null && !c.brands().isEmpty()) {
            params.addValue("brands", c.brands());
            where.append(" AND b.slug IN (:brands)");
        }
        if (c.minPrice() != null) {
            params.addValue("minPrice", c.minPrice());
            where.append(" AND p.min_price >= :minPrice");
        }
        if (c.maxPrice() != null) {
            params.addValue("maxPrice", c.maxPrice());
            where.append(" AND p.min_price <= :maxPrice");
        }
        if (c.minRating() != null) {
            params.addValue("minRating", c.minRating());
            where.append(" AND p.rating_average >= :minRating");
        }
        if (c.minDiscount() != null) {
            params.addValue("minDiscount", c.minDiscount());
            where.append(" AND p.discount_percent >= :minDiscount");
        }
        if (c.inStockOnly()) {
            where.append(" AND p.total_stock > 0");
        }
        return where.toString();
    }

    /**
     * Converts free text into a safe prefix tsquery: only alphanumeric tokens survive, so user
     * input can never inject tsquery operators.
     */
    static String toTsQuery(String raw) {
        if (raw == null) {
            return null;
        }
        List<String> tokens = Arrays.stream(raw.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(t -> !t.isBlank())
                .limit(8)
                .map(t -> t + ":*")
                .collect(Collectors.toCollection(ArrayList::new));
        return tokens.isEmpty() ? null : String.join(" & ", tokens);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static long num(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? 0 : ((Number) v).longValue();
    }
}

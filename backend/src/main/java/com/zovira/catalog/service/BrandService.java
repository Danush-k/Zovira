package com.zovira.catalog.service;

import com.zovira.catalog.dto.BrandResponse;
import com.zovira.common.exception.NotFoundException;
import com.zovira.config.CacheConfig;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {

    /** One pass over brands with their live product count and a representative product image. */
    private static final String LIST_SQL = """
            SELECT b.id, b.name, b.slug, b.logo_url, b.description, b.featured,
                   (SELECT pi.url
                      FROM products p
                      JOIN product_images pi ON pi.product_id = p.id
                     WHERE p.brand_id = b.id AND p.status = 'ACTIVE'
                     ORDER BY p.sold_count DESC, p.id, pi.position
                     LIMIT 1) AS image_url,
                   (SELECT count(*) FROM products p WHERE p.brand_id = b.id AND p.status = 'ACTIVE') AS product_count
              FROM brands b
             ORDER BY b.featured DESC, lower(b.name)
            """;

    private static final RowMapper<BrandResponse> MAPPER = (rs, i) -> new BrandResponse(rs.getLong("id"),
            rs.getString("name"), rs.getString("slug"), rs.getString("logo_url"), rs.getString("description"),
            rs.getBoolean("featured"), rs.getString("image_url"), rs.getLong("product_count"));

    private final NamedParameterJdbcTemplate jdbc;

    public BrandService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Cacheable(cacheNames = CacheConfig.BRANDS, key = "'all'")
    @Transactional(readOnly = true)
    public List<BrandResponse> list() {
        return jdbc.query(LIST_SQL, MAPPER).stream().filter(b -> b.productCount() > 0).toList();
    }

    public BrandResponse get(String slug) {
        return list().stream().filter(b -> b.slug().equals(slug)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Brand"));
    }
}

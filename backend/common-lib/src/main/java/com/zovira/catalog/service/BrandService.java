package com.zovira.catalog.service;

import com.zovira.catalog.dto.BrandResponse;
import com.zovira.catalog.entity.Brand;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.common.exception.NotFoundException;
import com.zovira.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BrandService {

    private final MongoTemplate mongoTemplate;

    public BrandService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Cacheable(cacheNames = CacheConfig.BRANDS, key = "'all'")
    public List<BrandResponse> list() {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "featured").and(Sort.by(Sort.Direction.ASC, "name")));
        List<Brand> brands = mongoTemplate.find(query, Brand.class);
        List<BrandResponse> responses = new ArrayList<>();

        for (Brand b : brands) {
            Query pQuery = new Query(Criteria.where("brandId").is(b.getId())
                    .and("status").is(ProductStatus.ACTIVE))
                    .with(Sort.by(Sort.Direction.DESC, "soldCount"));
            long count = mongoTemplate.count(pQuery, Product.class);
            if (count > 0) {
                Product topProduct = mongoTemplate.findOne(pQuery, Product.class);
                String imageUrl = null;
                if (topProduct != null && !topProduct.getImages().isEmpty()) {
                    imageUrl = topProduct.getImages().get(0).getUrl();
                } else {
                    imageUrl = b.getLogoUrl();
                }
                responses.add(new BrandResponse(
                        b.getId(),
                        b.getName(),
                        b.getSlug(),
                        b.getLogoUrl(),
                        b.getDescription(),
                        b.isFeatured(),
                        imageUrl,
                        count
                ));
            }
        }
        return responses;
    }

    public BrandResponse get(String slug) {
        return list().stream().filter(b -> b.slug().equals(slug)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Brand"));
    }
}

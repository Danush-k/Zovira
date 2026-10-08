package com.zovira.catalog.repository;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.inventory.entity.Inventory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ProductRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Product> findRail(ProductStatus status, int minStock, int minDiscount, int minRatings,
            boolean onlyWith3d, Pageable pageable) {
        Criteria criteria = Criteria.where("status").is(status)
                .and("totalStock").gte(minStock)
                .and("discountPercent").gte(minDiscount)
                .and("ratingCount").gte(minRatings);

        if (onlyWith3d) {
            criteria.and("modelUrl").ne(null);
        }

        Query query = new Query(criteria).with(pageable);
        return mongoTemplate.find(query, Product.class);
    }

    @Override
    public int adjustSoldCount(Long productId, int delta) {
        Query query = new Query(Criteria.where("_id").is(productId));
        Product p = mongoTemplate.findOne(query, Product.class);
        if (p != null) {
            int newSold = Math.max(0, p.getSoldCount() + delta);
            mongoTemplate.updateFirst(query, new Update().set("soldCount", newSold), Product.class);
            return 1;
        }
        return 0;
    }

    @Override
    public int refreshTotalStock(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }
        int updated = 0;
        for (Long id : productIds) {
            Product p = mongoTemplate.findById(id, Product.class);
            if (p != null) {
                int totalStock = 0;
                if (p.getVariants() != null) {
                    for (ProductVariant v : p.getVariants()) {
                        if (v.isActive()) {
                            Inventory inv = mongoTemplate.findOne(
                                    new Query(Criteria.where("variantId").is(v.getId())), Inventory.class);
                            if (inv != null) {
                                totalStock += inv.getQuantityAvailable();
                            }
                        }
                    }
                }
                mongoTemplate.updateFirst(
                        new Query(Criteria.where("_id").is(id)),
                        new Update().set("totalStock", totalStock),
                        Product.class);
                updated++;
            }
        }
        return updated;
    }
}

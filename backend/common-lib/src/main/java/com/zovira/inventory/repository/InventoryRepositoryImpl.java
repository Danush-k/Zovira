package com.zovira.inventory.repository;

import com.mongodb.client.result.UpdateResult;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.inventory.entity.Inventory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class InventoryRepositoryImpl implements InventoryRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public InventoryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public int reserve(Long variantId, int qty) {
        Query query = new Query(Criteria.where("variantId").is(variantId)
                .and("quantityAvailable").gte(qty));
        Update update = new Update()
                .inc("quantityAvailable", -qty)
                .inc("quantityReserved", qty)
                .inc("version", 1)
                .set("updatedAt", Instant.now());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Inventory.class);
        return (int) result.getModifiedCount();
    }

    @Override
    public int commitReservation(Long variantId, int qty) {
        Query query = new Query(Criteria.where("variantId").is(variantId));
        Inventory inv = mongoTemplate.findOne(query, Inventory.class);
        if (inv != null) {
            int newReserved = Math.max(0, inv.getQuantityReserved() - qty);
            Update update = new Update()
                    .set("quantityReserved", newReserved)
                    .inc("version", 1)
                    .set("updatedAt", Instant.now());
            UpdateResult result = mongoTemplate.updateFirst(query, update, Inventory.class);
            return (int) result.getModifiedCount();
        }
        return 0;
    }

    @Override
    public int releaseReservation(Long variantId, int qty) {
        Query query = new Query(Criteria.where("variantId").is(variantId));
        Inventory inv = mongoTemplate.findOne(query, Inventory.class);
        if (inv != null) {
            int released = Math.min(inv.getQuantityReserved(), qty);
            int newReserved = Math.max(0, inv.getQuantityReserved() - qty);
            Update update = new Update()
                    .inc("quantityAvailable", released)
                    .set("quantityReserved", newReserved)
                    .inc("version", 1)
                    .set("updatedAt", Instant.now());
            UpdateResult result = mongoTemplate.updateFirst(query, update, Inventory.class);
            return (int) result.getModifiedCount();
        }
        return 0;
    }

    @Override
    public int restock(Long variantId, int qty) {
        Query query = new Query(Criteria.where("variantId").is(variantId));
        Update update = new Update()
                .inc("quantityAvailable", qty)
                .inc("version", 1)
                .set("updatedAt", Instant.now());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Inventory.class);
        return (int) result.getModifiedCount();
    }

    @Override
    public int sumAvailableForProduct(Long productId) {
        Product product = mongoTemplate.findById(productId, Product.class);
        if (product == null || product.getVariants() == null) {
            return 0;
        }
        List<Long> variantIds = product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(ProductVariant::getId)
                .toList();
        if (variantIds.isEmpty()) {
            return 0;
        }
        Query query = new Query(Criteria.where("variantId").in(variantIds));
        List<Inventory> list = mongoTemplate.find(query, Inventory.class);
        return list.stream().mapToInt(Inventory::getQuantityAvailable).sum();
    }
}

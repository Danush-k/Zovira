package com.zovira.inventory.repository;

import com.zovira.inventory.entity.Inventory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<Inventory, Long>, InventoryRepositoryCustom {

    Optional<Inventory> findByVariantId(Long variantId);

    List<Inventory> findByVariantIdIn(Collection<Long> variantIds);
}

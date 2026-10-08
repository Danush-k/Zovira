package com.zovira.catalog.repository;

import java.util.List;
import java.util.Optional;
import com.zovira.catalog.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();
}

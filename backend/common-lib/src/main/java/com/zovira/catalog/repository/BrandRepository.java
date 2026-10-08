package com.zovira.catalog.repository;

import java.util.Optional;
import com.zovira.catalog.entity.Brand;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrandRepository extends MongoRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    Optional<Brand> findByNameIgnoreCase(String name);
}

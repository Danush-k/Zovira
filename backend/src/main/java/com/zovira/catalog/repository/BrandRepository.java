package com.zovira.catalog.repository;

import java.util.Optional;
import com.zovira.catalog.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    Optional<Brand> findByNameIgnoreCase(String name);
}

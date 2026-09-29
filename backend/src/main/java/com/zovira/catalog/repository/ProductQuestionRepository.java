package com.zovira.catalog.repository;


import com.zovira.catalog.entity.ProductQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {
}

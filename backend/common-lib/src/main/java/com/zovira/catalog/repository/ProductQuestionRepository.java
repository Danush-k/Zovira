package com.zovira.catalog.repository;

import com.zovira.catalog.entity.ProductQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductQuestionRepository extends MongoRepository<ProductQuestion, Long> {
}

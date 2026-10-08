package com.zovira.search.repository;

import com.zovira.search.entity.SearchHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchHistoryRepository extends MongoRepository<SearchHistory, Long>, SearchHistoryRepositoryCustom {
}

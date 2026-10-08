package com.zovira.search.repository;

import com.mongodb.client.result.DeleteResult;
import com.zovira.search.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class SearchHistoryRepositoryImpl implements SearchHistoryRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public SearchHistoryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<String> findRecentQueries(Long userId, Pageable pageable) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(pageable.getPageSize() * 2);
        List<SearchHistory> list = mongoTemplate.find(query, SearchHistory.class);
        Set<String> uniqueQueries = new LinkedHashSet<>();
        for (SearchHistory h : list) {
            uniqueQueries.add(h.getQuery());
            if (uniqueQueries.size() >= pageable.getPageSize()) {
                break;
            }
        }
        return new ArrayList<>(uniqueQueries);
    }

    @Override
    public int deleteByUserId(Long userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        DeleteResult result = mongoTemplate.remove(query, SearchHistory.class);
        return (int) result.getDeletedCount();
    }
}

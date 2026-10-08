package com.zovira.search.entity;

import com.zovira.common.entity.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "search_history")
public class SearchHistory extends BaseEntity {

    @Indexed
    private Long userId;

    private String query;
    private int resultCount;
    private Instant createdAt = Instant.now();

    public SearchHistory() {
    }

    public SearchHistory(Long userId, String query, int resultCount, Instant createdAt) {
        this.userId = userId;
        this.query = query;
        this.resultCount = resultCount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

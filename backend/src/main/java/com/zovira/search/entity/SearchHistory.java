package com.zovira.search.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "search_history")
public class SearchHistory extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 200, updatable = false)
    private String query;

    @Column(nullable = false)
    private int resultCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SearchHistory() {
    }

    public SearchHistory(Long userId, String query, int resultCount, Instant createdAt) {
        this.userId = userId;
        this.query = query;
        this.resultCount = resultCount;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getQuery() {
        return query;
    }

    public int getResultCount() {
        return resultCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

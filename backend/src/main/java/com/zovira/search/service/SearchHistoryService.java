package com.zovira.search.service;

import com.zovira.search.entity.SearchHistory;
import com.zovira.search.repository.SearchHistoryRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchHistoryService {

    private final SearchHistoryRepository repository;
    private final Clock clock;

    public SearchHistoryService(SearchHistoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Async
    @Transactional
    public void record(Long userId, String query, long resultCount) {
        String q = query.trim();
        if (q.length() < 2 || q.length() > 200) {
            return;
        }
        repository.save(new SearchHistory(userId, q, (int) Math.min(resultCount, Integer.MAX_VALUE), clock.instant()));
    }

    /** Distinct recent searches, newest first. */
    @Transactional(readOnly = true)
    public List<String> recent(Long userId) {
        return repository.findRecentQueries(userId, PageRequest.of(0, 8));
    }

    @Transactional
    public void clear(Long userId) {
        repository.deleteByUserId(userId);
    }
}

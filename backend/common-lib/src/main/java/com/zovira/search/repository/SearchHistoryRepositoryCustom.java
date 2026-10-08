package com.zovira.search.repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SearchHistoryRepositoryCustom {

    List<String> findRecentQueries(Long userId, Pageable pageable);

    int deleteByUserId(Long userId);
}

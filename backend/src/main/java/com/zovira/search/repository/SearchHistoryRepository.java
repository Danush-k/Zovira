package com.zovira.search.repository;

import com.zovira.search.entity.SearchHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    @Query("""
            select h.query from SearchHistory h
            where h.userId = :userId
            group by h.query
            order by max(h.createdAt) desc
            """)
    List<String> findRecentQueries(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("delete from SearchHistory h where h.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}

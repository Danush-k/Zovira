package com.zovira.search.controller;

import com.zovira.common.web.PageRequests;
import com.zovira.search.dto.SearchCriteria;
import com.zovira.search.dto.SearchResponse;
import com.zovira.search.dto.SearchSort;
import com.zovira.search.dto.SuggestResponse;
import com.zovira.search.service.ProductSearchService;
import com.zovira.search.service.SearchHistoryService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Search", description = "Product search, filtering, facets and autocomplete")
public class SearchController {

    private final ProductSearchService searchService;
    private final SearchHistoryService historyService;

    public SearchController(ProductSearchService searchService, SearchHistoryService historyService) {
        this.searchService = searchService;
        this.historyService = historyService;
    }

    @GetMapping("/api/v1/search")
    @Operation(summary = "Search and browse products with filters, sorting and facets")
    public SearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String seller,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer discount,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(defaultValue = "false") boolean has3d,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @CurrentUser(required = false) AuthUser user) {
        var pageable = PageRequests.of(page, size == null ? 24 : size);
        String query = q == null ? null : q.trim().substring(0, Math.min(q.trim().length(), 200));
        List<String> brands = brand == null || brand.isBlank() ? List.of()
                : Arrays.stream(brand.split(",")).map(String::trim).filter(s -> !s.isEmpty()).limit(20).toList();
        SearchCriteria criteria = new SearchCriteria(query, category, brands, seller, minPrice, maxPrice,
                clamp(rating, 1, 5), clamp(discount, 1, 90), inStock, has3d,
                SearchSort.parse(sort, query != null && !query.isBlank()), pageable.getPageNumber(),
                pageable.getPageSize());
        SearchResponse response = searchService.search(criteria);
        if (user != null && criteria.hasQuery() && criteria.page() == 0) {
            historyService.record(user.id(), query, response.results().totalElements());
        }
        return response;
    }

    @GetMapping("/api/v1/search/suggest")
    @Operation(summary = "Autocomplete suggestions for the search box")
    public SuggestResponse suggest(@RequestParam String q) {
        return searchService.suggest(q.length() > 100 ? q.substring(0, 100) : q);
    }

    @GetMapping("/api/v1/search/trending")
    @Operation(summary = "Popular searches this week")
    public List<String> trending() {
        return searchService.trending();
    }

    @GetMapping("/api/v1/users/me/search-history")
    @Operation(summary = "The signed-in user's recent searches")
    public List<String> history(@CurrentUser AuthUser user) {
        return historyService.recent(user.id());
    }

    @DeleteMapping("/api/v1/users/me/search-history")
    @Operation(summary = "Clear the signed-in user's search history")
    public ResponseEntity<Void> clearHistory(@CurrentUser AuthUser user) {
        historyService.clear(user.id());
        return ResponseEntity.noContent().build();
    }

    private static Integer clamp(Integer value, int min, int max) {
        return value == null ? null : Math.max(min, Math.min(max, value));
    }
}

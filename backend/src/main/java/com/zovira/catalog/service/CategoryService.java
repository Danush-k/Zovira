package com.zovira.catalog.service;

import com.zovira.catalog.dto.CategoryNode;
import com.zovira.catalog.dto.CategoryRef;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.repository.CategoryRepository;
import com.zovira.common.exception.NotFoundException;
import com.zovira.config.CacheConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryService self;

    public CategoryService(CategoryRepository repository, @Lazy CategoryService self) {
        this.repository = repository;
        this.self = self;
    }

    /** Active categories as a tree, ordered by sort order then name. Cached; evicted on admin edits. */
    @Cacheable(cacheNames = CacheConfig.CATEGORY_TREE, key = "'tree'")
    @Transactional(readOnly = true)
    public List<CategoryNode> tree() {
        List<Category> all = repository.findByActiveTrueOrderBySortOrderAscNameAsc();
        Map<Long, List<Category>> byParent = new HashMap<>();
        for (Category c : all) {
            Long parentId = c.getParent() == null ? null : c.getParent().getId();
            byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(c);
        }
        return build(null, byParent);
    }

    private List<CategoryNode> build(Long parentId, Map<Long, List<Category>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getName))
                .map(c -> new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getIcon(), c.getImageUrl(),
                        c.getDescription(), build(c.getId(), byParent)))
                .toList();
    }

    public Optional<CategoryNode> findNode(String slug) {
        Deque<CategoryNode> stack = new ArrayDeque<>(self.tree());
        while (!stack.isEmpty()) {
            CategoryNode node = stack.pop();
            if (node.slug().equals(slug)) {
                return Optional.of(node);
            }
            stack.addAll(node.children());
        }
        return Optional.empty();
    }

    public CategoryNode getNode(String slug) {
        return findNode(slug).orElseThrow(() -> NotFoundException.of("Category"));
    }

    /** The category and every category beneath it; used so browsing "Mobiles" includes "Smartphones". */
    public Set<Long> selfAndDescendantIds(String slug) {
        Set<Long> ids = new LinkedHashSet<>();
        Deque<CategoryNode> stack = new ArrayDeque<>();
        stack.push(getNode(slug));
        while (!stack.isEmpty()) {
            CategoryNode node = stack.pop();
            ids.add(node.id());
            node.children().forEach(stack::push);
        }
        return ids;
    }

    /** Root-to-leaf path for breadcrumbs. */
    public List<CategoryRef> path(Long categoryId) {
        List<CategoryRef> path = new ArrayList<>();
        if (!walk(self.tree(), categoryId, path)) {
            repository.findById(categoryId).ifPresent(c -> path.add(new CategoryRef(c.getId(), c.getName(), c.getSlug())));
        }
        return path;
    }

    private boolean walk(List<CategoryNode> nodes, Long targetId, List<CategoryRef> path) {
        for (CategoryNode node : nodes) {
            path.add(new CategoryRef(node.id(), node.name(), node.slug()));
            if (node.id().equals(targetId) || walk(node.children(), targetId, path)) {
                return true;
            }
            path.removeLast();
        }
        return false;
    }
}

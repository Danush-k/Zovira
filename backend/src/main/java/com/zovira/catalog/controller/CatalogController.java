package com.zovira.catalog.controller;

import com.zovira.catalog.dto.BrandResponse;
import com.zovira.catalog.dto.CategoryNode;
import com.zovira.catalog.dto.HomeResponse;
import com.zovira.catalog.dto.ProductDetailResponse;
import com.zovira.catalog.dto.StoreResponse;
import com.zovira.catalog.service.BrandService;
import com.zovira.catalog.service.CategoryService;
import com.zovira.catalog.service.HomeService;
import com.zovira.catalog.service.ProductQueryService;
import com.zovira.catalog.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Categories, brands, product pages and the storefront homepage")
public class CatalogController {

    private final CategoryService categoryService;
    private final BrandService brandService;
    private final HomeService homeService;
    private final ProductQueryService productQueryService;
    private final StoreService storeService;

    public CatalogController(CategoryService categoryService, BrandService brandService, HomeService homeService,
            ProductQueryService productQueryService, StoreService storeService) {
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.homeService = homeService;
        this.productQueryService = productQueryService;
        this.storeService = storeService;
    }

    @GetMapping("/home")
    @Operation(summary = "Homepage rails: deals, trending, best sellers, new arrivals, brands")
    public ResponseEntity<HomeResponse> home() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(homeService.home());
    }

    @GetMapping("/categories")
    @Operation(summary = "Category tree")
    public ResponseEntity<List<CategoryNode>> categories() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(categoryService.tree());
    }

    @GetMapping("/categories/{slug}")
    @Operation(summary = "A category with its sub-categories")
    public CategoryNode category(@PathVariable String slug) {
        return categoryService.getNode(slug);
    }

    @GetMapping("/brands")
    @Operation(summary = "Brands with at least one active product")
    public List<BrandResponse> brands() {
        return brandService.list();
    }

    @GetMapping("/brands/{slug}")
    @Operation(summary = "A brand")
    public BrandResponse brand(@PathVariable String slug) {
        return brandService.get(slug);
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Product page with variants, specifications and live stock")
    public ProductDetailResponse product(@PathVariable String slug) {
        return productQueryService.detail(slug, true);
    }

    @GetMapping("/products/compare")
    @Operation(summary = "Compare up to four products side by side")
    public List<ProductDetailResponse> compare(@RequestParam("slugs") String slugs) {
        return productQueryService.compare(Arrays.stream(slugs.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList());
    }

    @GetMapping("/stores/{slug}")
    @Operation(summary = "A seller's public storefront")
    public StoreResponse store(@PathVariable String slug) {
        return storeService.store(slug);
    }
}

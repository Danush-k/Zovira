package com.zovira.catalog.service;

import com.zovira.catalog.dto.HeroSlide;
import com.zovira.catalog.dto.HomeResponse;
import com.zovira.catalog.dto.ProductSummary;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.mapper.CatalogMapper;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.config.CacheConfig;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles the storefront homepage rails. Cached briefly since it is the most requested page. */
@Service
public class HomeService {

    private static final int RAIL_SIZE = 12;

    /** Campaign banners. Each references a product whose primary image is used as the visual. */
    private record Campaign(String productSlug, String eyebrow, String title, String subtitle, String ctaLabel,
            String ctaLink, String tone) {
    }

    private static final List<Campaign> CAMPAIGNS = List.of(
            new Campaign("apple-iphone-16-pro", "New launch", "The iPhone 16 lineup has landed",
                    "A18 chips, Camera Control and all-day battery life. From \u20b979,900.", "Shop iPhone 16",
                    "/search?q=iphone%2016", "sand"),
            new Campaign("nordhaus-glam-velvet-curved-sofa", "Only on Zovira", "See it in your room before you buy",
                    "Explore furniture and decor in 3D, then place it in your space with AR.", "Explore 3D and AR",
                    "/search?has3d=true", "teal"),
            new Campaign("dell-xps-13-9300", "Laptops", "Power through work, study and play",
                    "MacBook, XPS, Zenbook and more, with free delivery and easy returns.", "Shop laptops",
                    "/c/laptops", "lilac"),
            new Campaign("atomic-habits-paperback", "Books", "Bestselling reads from \u20b9279",
                    "Business, technology and fiction picks, shipped in protective packaging.", "Browse books",
                    "/c/books", "mint"));

    private final ProductRepository products;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public HomeService(ProductRepository products, CategoryService categoryService, BrandService brandService) {
        this.products = products;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @Cacheable(cacheNames = CacheConfig.HOME, key = "'home'")
    @Transactional(readOnly = true)
    public HomeResponse home() {
        return new HomeResponse(
                hero(),
                categoryService.tree(),
                rail(1, 15, 0, false, Sort.by(Sort.Order.desc("discountPercent"), Sort.Order.desc("soldCount"))),
                rail(1, 0, 0, false, Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"))),
                rail(0, 0, 0, false, Sort.by(Sort.Order.desc("soldCount"), Sort.Order.desc("id"))),
                rail(0, 0, 0, false, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))),
                rail(0, 0, 3, false, Sort.by(Sort.Order.desc("ratingAverage"), Sort.Order.desc("ratingCount"))),
                rail(0, 0, 0, true, Sort.by(Sort.Order.desc("soldCount"))),
                brandService.list().stream().filter(b -> b.featured() && b.imageUrl() != null).limit(10).toList());
    }

    private List<HeroSlide> hero() {
        return CAMPAIGNS.stream()
                .flatMap(c -> products.findDetailBySlug(c.productSlug())
                        .filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.primaryImageUrl() != null)
                        .map(p -> new HeroSlide(c.eyebrow(), c.title(), c.subtitle(), c.ctaLabel(), c.ctaLink(),
                                p.primaryImageUrl(), c.tone()))
                        .stream())
                .toList();
    }

    private List<ProductSummary> rail(int minStock, int minDiscount, int minRatings, boolean with3d, Sort sort) {
        return products.findRail(ProductStatus.ACTIVE, minStock, minDiscount, minRatings, with3d,
                        PageRequest.of(0, RAIL_SIZE, sort))
                .stream()
                .map(CatalogMapper::toSummary)
                .toList();
    }
}

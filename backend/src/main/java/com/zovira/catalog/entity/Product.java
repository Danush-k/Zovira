package com.zovira.catalog.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.seller.entity.Seller;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, updatable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> highlights = new ArrayList<>();

    @Column(length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minMrp = BigDecimal.ZERO;

    @Column(nullable = false)
    private int discountPercent;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(nullable = false)
    private int ratingCount;

    @Column(nullable = false)
    private int totalStock;

    @Column(nullable = false)
    private int soldCount;

    @Column(nullable = false)
    private long viewCount;

    @Column(length = 200)
    private String warranty;

    @Column(nullable = false)
    private boolean returnable = true;

    @Column(nullable = false)
    private int returnWindowDays = 7;

    @Column(nullable = false)
    private boolean codAvailable = true;

    @Column(length = 512)
    private String modelUrl;

    @Column(length = 512)
    private String arModelUrl;

    @Column(length = 512)
    private String modelPosterUrl;

    @Column(length = 1000)
    private String searchKeywords;

    private Instant publishedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ProductSpecification> specifications = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ProductAttribute> attributes = new ArrayList<>();

    protected Product() {
    }

    public Product(Seller seller, Category category, Brand brand, String title, String slug) {
        this.seller = seller;
        this.category = category;
        this.brand = brand;
        this.title = title;
        this.slug = slug;
    }

    public boolean isPurchasable() {
        return status == ProductStatus.ACTIVE;
    }

    public void publish(Instant now) {
        this.status = ProductStatus.ACTIVE;
        if (publishedAt == null) {
            publishedAt = now;
        }
    }

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        variants.add(variant);
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }

    public void addSpecification(ProductSpecification spec) {
        spec.setProduct(this);
        specifications.add(spec);
    }

    public void addAttribute(ProductAttribute attribute) {
        attribute.setProduct(this);
        attributes.add(attribute);
    }

    public ProductVariant defaultVariant() {
        return variants.stream().filter(v -> v.isActive() && v.isDefaultVariant()).findFirst()
                .orElseGet(() -> variants.stream().filter(ProductVariant::isActive)
                        .min(Comparator.comparing(ProductVariant::getPrice)).orElse(null));
    }

    public String primaryImageUrl() {
        return images.isEmpty() ? null : images.getFirst().getUrl();
    }

    /** Recomputes the denormalized price fields from the active variants. */
    public void refreshPricing() {
        variants.stream().filter(ProductVariant::isActive)
                .min(Comparator.comparing(ProductVariant::getPrice))
                .ifPresentOrElse(cheapest -> {
                    this.minPrice = cheapest.getPrice();
                    this.minMrp = cheapest.getMrp();
                    this.discountPercent = discountPercent(cheapest.getPrice(), cheapest.getMrp());
                }, () -> {
                    this.minPrice = BigDecimal.ZERO;
                    this.minMrp = BigDecimal.ZERO;
                    this.discountPercent = 0;
                });
    }

    public static int discountPercent(BigDecimal price, BigDecimal mrp) {
        if (mrp == null || mrp.signum() <= 0 || price.compareTo(mrp) >= 0) {
            return 0;
        }
        return mrp.subtract(price).multiply(BigDecimal.valueOf(100))
                .divide(mrp, 0, RoundingMode.HALF_UP).intValue();
    }

    public void updateRating(BigDecimal average, int count) {
        this.ratingAverage = average;
        this.ratingCount = count;
    }

    public void incrementSold(int quantity) {
        this.soldCount += quantity;
    }

    public Seller getSeller() {
        return seller;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights == null ? new ArrayList<>() : new ArrayList<>(highlights);
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMinMrp() {
        return minMrp;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public long getViewCount() {
        return viewCount;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public boolean isReturnable() {
        return returnable;
    }

    public void setReturnable(boolean returnable) {
        this.returnable = returnable;
    }

    public int getReturnWindowDays() {
        return returnWindowDays;
    }

    public void setReturnWindowDays(int returnWindowDays) {
        this.returnWindowDays = returnWindowDays;
    }

    public boolean isCodAvailable() {
        return codAvailable;
    }

    public void setCodAvailable(boolean codAvailable) {
        this.codAvailable = codAvailable;
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }

    public String getArModelUrl() {
        return arModelUrl;
    }

    public void setArModelUrl(String arModelUrl) {
        this.arModelUrl = arModelUrl;
    }

    public String getModelPosterUrl() {
        return modelPosterUrl;
    }

    public void setModelPosterUrl(String modelPosterUrl) {
        this.modelPosterUrl = modelPosterUrl;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public List<ProductSpecification> getSpecifications() {
        return specifications;
    }

    public List<ProductAttribute> getAttributes() {
        return attributes;
    }
}

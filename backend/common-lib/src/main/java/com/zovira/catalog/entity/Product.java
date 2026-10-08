package com.zovira.catalog.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.seller.entity.Seller;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Document(collection = "products")
public class Product extends AuditableEntity {

    private Seller seller;

    @Indexed
    private Long sellerId;

    private Category category;

    @Indexed
    private Long categoryId;

    private Brand brand;

    @Indexed
    private Long brandId;

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String shortDescription;
    private String description;
    private List<String> highlights = new ArrayList<>();
    private String tags;
    private ProductStatus status = ProductStatus.DRAFT;
    private boolean featured;
    private BigDecimal minPrice = BigDecimal.ZERO;
    private BigDecimal minMrp = BigDecimal.ZERO;
    private int discountPercent;
    private BigDecimal ratingAverage = BigDecimal.ZERO;
    private int ratingCount;
    private int totalStock;
    private int soldCount;
    private long viewCount;
    private String warranty;
    private boolean returnable = true;
    private int returnWindowDays = 7;
    private boolean codAvailable = true;
    private String modelUrl;
    private String arModelUrl;
    private String modelPosterUrl;
    private String searchKeywords;
    private Instant publishedAt;

    @Version
    private Long version;

    private List<ProductVariant> variants = new ArrayList<>();
    private List<ProductImage> images = new ArrayList<>();
    private List<ProductSpecification> specifications = new ArrayList<>();
    private List<ProductAttribute> attributes = new ArrayList<>();

    public Product() {
    }

    public Product(Seller seller, Category category, Brand brand, String title, String slug) {
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
        this.category = category;
        this.categoryId = category != null ? category.getId() : null;
        this.brand = brand;
        this.brandId = brand != null ? brand.getId() : null;
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

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    public Long getSellerId() {
        if (sellerId != null) return sellerId;
        return seller != null ? seller.getId() : null;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        this.categoryId = category != null ? category.getId() : null;
    }

    public Long getCategoryId() {
        if (categoryId != null) return categoryId;
        return category != null ? category.getId() : null;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
        this.brandId = brand != null ? brand.getId() : null;
    }

    public Long getBrandId() {
        if (brandId != null) return brandId;
        return brand != null ? brand.getId() : null;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
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

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMinMrp() {
        return minMrp;
    }

    public void setMinMrp(BigDecimal minMrp) {
        this.minMrp = minMrp;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(BigDecimal ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
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

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
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

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants == null ? new ArrayList<>() : variants;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images == null ? new ArrayList<>() : images;
    }

    public List<ProductSpecification> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<ProductSpecification> specifications) {
        this.specifications = specifications == null ? new ArrayList<>() : specifications;
    }

    public List<ProductAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ProductAttribute> attributes) {
        this.attributes = attributes == null ? new ArrayList<>() : attributes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

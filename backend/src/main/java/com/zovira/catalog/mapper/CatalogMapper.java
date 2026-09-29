package com.zovira.catalog.mapper;

import com.zovira.catalog.dto.AttributeResponse;
import com.zovira.catalog.dto.BrandRef;
import com.zovira.catalog.dto.CategoryRef;
import com.zovira.catalog.dto.ProductDetailResponse;
import com.zovira.catalog.dto.ProductImageResponse;
import com.zovira.catalog.dto.ProductModel;
import com.zovira.catalog.dto.ProductSummary;
import com.zovira.catalog.dto.SellerSummary;
import com.zovira.catalog.dto.SpecGroup;
import com.zovira.catalog.dto.VariantResponse;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductSpecification;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.inventory.entity.Inventory;
import com.zovira.seller.entity.Seller;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CatalogMapper {

    public static final String IN_STOCK = "IN_STOCK";
    public static final String LOW_STOCK = "LOW_STOCK";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";

    private CatalogMapper() {
    }

    public static ProductSummary toSummary(Product p) {
        ProductVariant def = p.defaultVariant();
        return new ProductSummary(p.getId(), p.getSlug(), p.getTitle(),
                p.getBrand() == null ? null : p.getBrand().getName(), p.primaryImageUrl(), p.getMinPrice(),
                p.getMinMrp(), p.getDiscountPercent(), p.getRatingAverage(), p.getRatingCount(),
                p.getTotalStock() > 0, p.getModelUrl() != null, def == null ? null : def.getId(),
                (int) p.getVariants().stream().filter(ProductVariant::isActive).count());
    }

    public static SellerSummary toSellerSummary(Seller s) {
        return new SellerSummary(s.getId(), s.getStoreName(), s.getSlug(), s.getRatingAverage(), s.getRatingCount(),
                s.getPickupCity(), s.getPickupState());
    }

    public static CategoryRef toRef(Category c) {
        return new CategoryRef(c.getId(), c.getName(), c.getSlug());
    }

    /** Builds the cacheable part of a product page; stock is filled in per request by {@link #withStock}. */
    public static ProductDetailResponse toDetail(Product p, List<CategoryRef> breadcrumbs) {
        ProductVariant def = p.defaultVariant();
        List<VariantResponse> variants = p.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(v -> new VariantResponse(v.getId(), v.getSku(), v.getName(), v.getOptions(), v.getPrice(),
                        v.getMrp(), Product.discountPercent(v.getPrice(), v.getMrp()), OUT_OF_STOCK, 0, null,
                        def != null && def.getId().equals(v.getId())))
                .toList();

        Map<String, List<SpecGroup.SpecItem>> groups = new LinkedHashMap<>();
        for (ProductSpecification s : p.getSpecifications()) {
            groups.computeIfAbsent(s.getGroupName(), k -> new ArrayList<>())
                    .add(new SpecGroup.SpecItem(s.getName(), s.getValue()));
        }

        return new ProductDetailResponse(p.getId(), p.getSlug(), p.getTitle(), p.getShortDescription(),
                p.getDescription(), List.copyOf(p.getHighlights()),
                p.getBrand() == null ? null : new BrandRef(p.getBrand().getId(), p.getBrand().getName(),
                        p.getBrand().getSlug()),
                toRef(p.getCategory()), breadcrumbs, toSellerSummary(p.getSeller()),
                p.getImages().stream()
                        .map(i -> new ProductImageResponse(i.getId(), i.getUrl(), i.getAltText(),
                                i.getVariant() == null ? null : i.getVariant().getId()))
                        .toList(),
                p.getAttributes().stream()
                        .map(a -> new AttributeResponse(a.getName(), List.copyOf(a.getOptionValues())))
                        .toList(),
                variants, def == null ? null : def.getId(),
                groups.entrySet().stream().map(e -> new SpecGroup(e.getKey(), e.getValue())).toList(),
                p.getRatingAverage(), p.getRatingCount(), p.getWarranty(), p.isReturnable(),
                p.getReturnWindowDays(), p.isCodAvailable(), p.getCategory().getTaxRate(),
                p.getModelUrl() == null ? null
                        : new ProductModel(p.getModelUrl(), p.getArModelUrl(), p.getModelPosterUrl()),
                p.getStatus().name(), tags(p.getTags()));
    }

    public static ProductDetailResponse withStock(ProductDetailResponse detail, Map<Long, Inventory> inventory,
            int maxPerItem) {
        List<VariantResponse> variants = detail.variants().stream().map(v -> {
            Inventory inv = inventory.get(v.id());
            int available = inv == null ? 0 : inv.getQuantityAvailable();
            String status = available <= 0 ? OUT_OF_STOCK
                    : available <= (inv.getLowStockThreshold()) ? LOW_STOCK : IN_STOCK;
            return v.withStock(status, Math.min(available, maxPerItem), LOW_STOCK.equals(status) ? available : null);
        }).toList();
        return detail.withVariants(variants);
    }

    public static Map<Long, Inventory> byVariant(List<Inventory> inventory) {
        return inventory.stream().collect(Collectors.toMap(i -> i.getVariant().getId(), i -> i));
    }

    private static List<String> tags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}

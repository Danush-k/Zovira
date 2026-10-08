package com.zovira.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String slug,
        String title,
        String shortDescription,
        String description,
        List<String> highlights,
        BrandRef brand,
        CategoryRef category,
        List<CategoryRef> breadcrumbs,
        SellerSummary seller,
        List<ProductImageResponse> images,
        List<AttributeResponse> attributes,
        List<VariantResponse> variants,
        Long defaultVariantId,
        List<SpecGroup> specifications,
        BigDecimal ratingAverage,
        int ratingCount,
        String warranty,
        boolean returnable,
        int returnWindowDays,
        boolean codAvailable,
        BigDecimal taxRate,
        ProductModel model,
        String status,
        List<String> tags) {

    public ProductDetailResponse withVariants(List<VariantResponse> updated) {
        return new ProductDetailResponse(id, slug, title, shortDescription, description, highlights, brand, category,
                breadcrumbs, seller, images, attributes, updated, defaultVariantId, specifications, ratingAverage,
                ratingCount, warranty, returnable, returnWindowDays, codAvailable, taxRate, model, status, tags);
    }
}

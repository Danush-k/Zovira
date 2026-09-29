package com.zovira.seed;

import com.zovira.catalog.entity.AttributeOption;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** JSON shapes of the files under {@code classpath:seed/}. */
final class SeedModels {

    private SeedModels() {
    }

    record CatalogSeed(List<CategorySeed> categories, List<SellerSeed> sellers, List<BrandSeed> brands,
            List<ProductSeed> products) {
    }

    record CategorySeed(String name, String slug, String icon, BigDecimal taxRate, String description,
            List<CategorySeed> children) {
    }

    record SellerSeed(String key, String storeName, String fullName, String email, String city, String state,
            String pincode, String line1, String gstin, String phone, String description) {
    }

    record BrandSeed(String name, String slug, boolean featured) {
    }

    record ProductSeed(String title, String slug, String category, String brand, String seller,
            String shortDescription, String description, List<String> highlights, List<String> images,
            Map<String, List<String>> variantImages, List<List<String>> specs, String warranty, boolean returnable,
            int returnWindowDays, boolean cod, String tags, Boolean featured, Integer soldCount, Integer viewCount,
            ModelSeed model, List<AttributeSeed> attributes, List<VariantSeed> variants, List<ReviewSeed> reviews) {
    }

    record ModelSeed(String url, String poster) {
    }

    record AttributeSeed(String name, List<AttributeOption> options) {
    }

    record VariantSeed(String sku, String name, Map<String, String> options, BigDecimal price, BigDecimal mrp,
            int stock, boolean isDefault) {
    }

    record ReviewSeed(int customer, int rating, String title, String body, int daysAgo) {
    }

    record PeopleSeed(AdminSeed admin, String sellerPassword, String customerPassword, PendingSellerSeed pendingSeller,
            List<CustomerSeed> customers) {
    }

    record AdminSeed(String fullName, String email, String password) {
    }

    record PendingSellerSeed(String fullName, String email, String storeName, String description, String city,
            String state, String pincode, String line1, String gstin, String phone) {
    }

    record CustomerSeed(String fullName, String email, String phone, String line1, String line2, String city,
            String state, String pincode) {
    }
}

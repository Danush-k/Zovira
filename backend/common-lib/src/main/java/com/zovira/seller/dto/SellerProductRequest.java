package com.zovira.seller.dto;

import com.zovira.catalog.entity.AttributeOption;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Create or replace a seller's listing, including its options, variants and content. */
public record SellerProductRequest(
        @NotBlank(message = "Enter a product title") @Size(min = 5, max = 200) String title,
        @NotNull(message = "Choose a category") Long categoryId,
        Long brandId,
        @Size(max = 120) String newBrandName,
        @Size(max = 500) String shortDescription,
        @Size(max = 20000) String description,
        @Size(max = 10) List<@NotBlank @Size(max = 200) String> highlights,
        @Size(max = 500) String tags,
        @Size(max = 200) String warranty,
        boolean returnable,
        @Min(0) @Max(90) int returnWindowDays,
        boolean codAvailable,
        @Size(max = 512) String modelUrl,
        @Size(max = 512) String arModelUrl,
        @Size(max = 10) List<@Valid AttributeInput> attributes,
        @NotEmpty(message = "Add at least one variant") @Size(max = 60) List<@Valid VariantInput> variants,
        @Size(max = 12) List<@Valid ImageInput> images,
        @Size(max = 60) List<@Valid SpecInput> specifications,
        boolean publish) {

    public record AttributeInput(@NotBlank @Size(max = 40) String name,
            @NotEmpty @Size(max = 30) List<AttributeOption> options) {
    }

    public record VariantInput(
            Long id,
            @Size(max = 64) String sku,
            @Size(max = 160) String name,
            Map<String, String> options,
            @NotNull @DecimalMin(value = "1.00", message = "Price must be at least 1") BigDecimal price,
            @NotNull @DecimalMin("1.00") BigDecimal mrp,
            @Min(0) @Max(100000) int stock,
            Integer weightGrams,
            boolean isDefault,
            boolean active) {
    }

    public record ImageInput(@NotBlank @Size(max = 512) String url, @Size(max = 200) String altText, String variantOption) {
    }

    public record SpecInput(@Size(max = 60) String group, @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 500) String value) {
    }
}

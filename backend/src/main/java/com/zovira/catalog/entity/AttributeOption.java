package com.zovira.catalog.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One selectable value of a product attribute. {@code swatch} is an optional hex colour for colour
 * chips; {@code modelVariant} names the matching material variant in the product's 3D model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttributeOption(String value, String swatch, String modelVariant) {

    public AttributeOption(String value) {
        this(value, null, null);
    }
}

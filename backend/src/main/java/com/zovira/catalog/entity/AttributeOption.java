package com.zovira.catalog.entity;

/**
 * One selectable value of a product attribute. {@code swatch} is an optional hex color used to
 * render color chips.
 */
public record AttributeOption(String value, String swatch) {
}

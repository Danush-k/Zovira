package com.zovira.catalog.dto;

/** A homepage promotional banner resolved against live catalog data. */
public record HeroSlide(
        String eyebrow,
        String title,
        String subtitle,
        String ctaLabel,
        String ctaLink,
        String imageUrl,
        String tone) {
}

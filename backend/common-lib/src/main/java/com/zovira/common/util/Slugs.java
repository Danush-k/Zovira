package com.zovira.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private Slugs() {
    }

    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD);
        String ascii = DIACRITICS.matcher(normalized).replaceAll("").toLowerCase(Locale.ROOT);
        String slug = NON_ALNUM.matcher(ascii).replaceAll("-").replaceAll("(^-+|-+$)", "");
        if (slug.length() > 180) {
            slug = slug.substring(0, 180).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "item" : slug;
    }

    /** Appends -2, -3, ... until {@code taken} reports the slug as free. */
    public static String unique(String input, Predicate<String> taken) {
        String base = slugify(input);
        String candidate = base;
        int suffix = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}

package com.zovira.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void slugifiesTitles() {
        assertThat(Slugs.slugify("Apple iPhone 16 Pro Max (256 GB) - Desert Titanium"))
                .isEqualTo("apple-iphone-16-pro-max-256-gb-desert-titanium");
    }

    @Test
    void stripsDiacritics() {
        assertThat(Slugs.slugify("Crème Brûlée Café")).isEqualTo("creme-brulee-cafe");
    }

    @Test
    void appendsSuffixUntilUnique() {
        Set<String> taken = Set.of("wireless-mouse", "wireless-mouse-2");
        assertThat(Slugs.unique("Wireless Mouse", taken::contains)).isEqualTo("wireless-mouse-3");
    }

    @Test
    void neverReturnsEmpty() {
        assertThat(Slugs.slugify("!!!")).isEqualTo("item");
    }
}

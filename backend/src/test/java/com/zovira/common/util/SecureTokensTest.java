package com.zovira.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecureTokensTest {

    @Test
    void generatesUniqueUrlSafeTokens() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = SecureTokens.generate();
            assertThat(token).matches("[A-Za-z0-9_-]{43}");
            assertThat(seen.add(token)).isTrue();
        }
    }

    @Test
    void hashesDeterministicallyToHex() {
        assertThat(SecureTokens.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void referencesAvoidAmbiguousCharacters() {
        String ref = SecureTokens.reference("ZV", 10);
        assertThat(ref).startsWith("ZV-").hasSize(13).doesNotContainPattern("[01IOL]");
    }
}

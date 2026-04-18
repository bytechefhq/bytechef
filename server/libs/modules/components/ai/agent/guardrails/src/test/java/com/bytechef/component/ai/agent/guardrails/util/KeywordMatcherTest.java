/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agent.guardrails.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcher.KeywordMatchResult;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeywordMatcherTest {

    private Locale originalLocale;

    @BeforeEach
    void captureLocale() {
        originalLocale = Locale.getDefault();
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void testMatchCaseInsensitive() {
        KeywordMatchResult result = KeywordMatcher.match("Hello WORLD", List.of("world"), false);

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedKeywords()).containsExactly("world");
    }

    @Test
    void testMatchCaseSensitiveMiss() {
        KeywordMatchResult result = KeywordMatcher.match("Hello WORLD", List.of("world"), true);

        assertThat(result.matched()).isFalse();
    }

    @Test
    void testMatchCaseSensitiveHit() {
        KeywordMatchResult result = KeywordMatcher.match("Hello world", List.of("world"), true);

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedKeywords()).containsExactly("world");
    }

    @Test
    void testMatchEmptyKeywords() {
        KeywordMatchResult result = KeywordMatcher.match("anything", List.of(), false);

        assertThat(result.matched()).isFalse();
    }

    @Test
    void testMatchUnderTurkishLocaleStillFindsCapitalI() {
        // Regression test for Turkish-I bug: with tr_TR default locale, "I".toLowerCase() -> "ı", which
        // would silently break case-insensitive matching. Locale.ROOT fix should make this pass.
        Locale.setDefault(new Locale("tr", "TR"));

        KeywordMatchResult result = KeywordMatcher.match("HELLO INTERNET", List.of("internet"));

        assertThat(result.matched()).isTrue();
    }

    @Test
    void testMaskCaseSensitiveDoesNotMaskDifferentCase() {
        String result = KeywordMatcher.mask("Hello WORLD and world", List.of("world"), true);

        // Only lowercase "world" should be masked; "WORLD" untouched.
        assertThat(result).isEqualTo("Hello WORLD and *****");
    }

    @Test
    void testMaskCaseInsensitiveMasksBothCases() {
        String result = KeywordMatcher.mask("Hello WORLD and world", List.of("world"), false);

        assertThat(result).isEqualTo("Hello ***** and *****");
    }

    @Test
    void testMaskDefaultOverloadIsCaseInsensitive() {
        String result = KeywordMatcher.mask("Hello WORLD", List.of("world"));

        assertThat(result).isEqualTo("Hello *****");
    }

    @Test
    void testMaskEmptyInputs() {
        assertThat(KeywordMatcher.mask("", List.of("x"))).isEmpty();
        assertThat(KeywordMatcher.mask(null, List.of("x"))).isNull();
        assertThat(KeywordMatcher.mask("abc", List.of())).isEqualTo("abc");
    }

    @Test
    void testMatchesWholeWordWithUnicodeLetterBoundary() {
        KeywordMatchResult result = KeywordMatcher.match(
            "Das Geschäft läuft gut", List.of("Geschäft"), false);

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedKeywords()).containsExactly("Geschäft");
    }

    @Test
    void testDoesNotMatchAsSubstringOfLargerUnicodeWord() {
        KeywordMatchResult result = KeywordMatcher.match(
            "Das Geschäftsmodell wächst", List.of("Geschäft"), false);

        assertThat(result.matched()).isFalse();
    }

    @Test
    void testParseKeywordsStripsTrailingPunctuationFromCommaList() {
        List<String> keywords = KeywordMatcher.parseKeywords("hello, world!");

        assertThat(keywords).containsExactly("hello", "world");
    }

    @Test
    void testParseKeywordsHandlesNullAndBlank() {
        assertThat(KeywordMatcher.parseKeywords(null)).isEmpty();
        assertThat(KeywordMatcher.parseKeywords("   ")).isEmpty();
        assertThat(KeywordMatcher.parseKeywords(",,,")).isEmpty();
    }

    @Test
    void testMaskAndMatchAgreeOnWordBoundary() {
        // match("programming", ["program"]) returns false — "program" is a substring of a larger word. mask should
        // behave consistently and NOT mask substrings of larger words.
        assertThat(KeywordMatcher.match("programming", List.of("program"), false)
            .matched()).isFalse();
        assertThat(KeywordMatcher.mask("programming", List.of("program"))).isEqualTo("programming");
    }

    @Test
    void testMaskDoesNotMaskSubstringOfUnicodeWord() {
        assertThat(KeywordMatcher.mask("Geschäftsmodell", List.of("Geschäft")))
            .isEqualTo("Geschäftsmodell");
    }

    @Test
    void testZeroWidthCharacterBypassIsDocumentedLimitation() {
        // Zero-width space U+200B between characters of a keyword is NOT recognised by the current boundary-aware
        // matcher — Pattern.quote treats the input literally, so "jail\u200Bbreak" is seen as a different string.
        // This test pins the known limitation so a future refactor that adds Unicode normalisation (NFKC) updates the
        // assertion explicitly instead of silently shifting behaviour. Documented in the security considerations for
        // KeywordMatcher: operators who need this defense should apply NFKC-stripping upstream of the detector.
        String zeroWidthInput = "jail\u200Bbreak attempt";

        KeywordMatchResult result = KeywordMatcher.match(zeroWidthInput, List.of("jailbreak"), false);

        assertThat(result.matched())
            .as("zero-width bypass of literal keyword is NOT defended against today — pin limitation")
            .isFalse();
    }

    @Test
    void testCyrillicHomoglyphBypassIsDocumentedLimitation() {
        // Cyrillic "а" (U+0430) is visually identical to Latin "a" (U+0061). KeywordMatcher matches codepoints, so a
        // keyword literal "assistant" against a Cyrillic-homoglyph input "аssistant" will not match. Pinning so a
        // future homoglyph-folding pass is an explicit change, not a silent behaviour shift.
        String homoglyphInput = "\u0430ssistant instructions";

        KeywordMatchResult result = KeywordMatcher.match(homoglyphInput, List.of("assistant"), false);

        assertThat(result.matched())
            .as("Cyrillic homoglyph bypass is NOT defended against today — pin limitation")
            .isFalse();
    }
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaskEntityMapTest {

    @Test
    void testEmptyByDefault() {
        MaskEntityMap map = new MaskEntityMap();

        assertThat(map.isEmpty()).isTrue();
        assertThat(map.applyTo("some text")).isEqualTo("some text");
    }

    @Test
    void testMergeNullOrEmptyAdditionsIsNoOp() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(null);
        map.merge(Map.of());

        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    void testMergeAccumulatesMultipleMergesOfSameType() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of("a@b.com")));
        map.merge(Map.of("EMAIL", List.of("c@d.com")));

        String result = map.applyTo("contact a@b.com or c@d.com for help");

        assertThat(result).isEqualTo("contact <EMAIL> or <EMAIL> for help");
    }

    @Test
    void testApplyToReplacesAllOccurrencesOfAValue() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of("a@b.com")));

        String result = map.applyTo("a@b.com and again a@b.com");

        assertThat(result).isEqualTo("<EMAIL> and again <EMAIL>");
    }

    @Test
    void testApplyToSkipsNullAndEmptyValues() {
        // Null / empty values must not cause infinite replacement or NPE — the detector may emit entries that turn out
        // to be structurally empty.
        Map<String, List<String>> additions = new HashMap<>();

        List<String> values = new java.util.ArrayList<>();

        values.add(null);
        values.add("");
        values.add("real");
        additions.put("X", values);

        MaskEntityMap map = new MaskEntityMap();

        map.merge(additions);

        String result = map.applyTo("the real thing");

        assertThat(result).isEqualTo("the <X> thing");
    }

    @Test
    void testApplyToSkipsEmptyValueListsForAType() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of()));

        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    void testApplyToLongestEntityWinsRegardlessOfInsertionOrder() {
        MaskEntityMap map = new MaskEntityMap();

        // Insert the shorter value first; applyTo must still prefer the longer substring so "alice@corp.com" is
        // replaced whole instead of being split to "<URL>corp.com" by a naive ordering.
        map.merge(Map.of("URL", List.of("corp.com")));
        map.merge(Map.of("EMAIL", List.of("alice@corp.com")));

        String result = map.applyTo("write to alice@corp.com today");

        assertThat(result).isEqualTo("write to <EMAIL> today");
    }

    @Test
    void testApplyToLeavesTextUnchangedWhenEntityValueIsAbsentFromText() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of("nowhere@nope.com")));

        String result = map.applyTo("this text has no email");

        assertThat(result).isEqualTo("this text has no email");
    }

    @Test
    void testApplyToNullOrEmptyTextReturnsInput() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of("x@y.com")));

        assertThat(map.applyTo(null)).isNull();
        assertThat(map.applyTo("")).isEmpty();
    }

    @Test
    void testApplyToHandlesSubstringOverlapWhereShorterIsPrefixOfLonger() {
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("SHORT", List.of("abc"), "LONG", List.of("abcdef")));

        String result = map.applyTo("xx abcdef yy abc zz");

        // "abcdef" is longer, so wins in its position. After that replacement "abc" in "zz" position still matches.
        assertThat(result).isEqualTo("xx <LONG> yy <SHORT> zz");
    }

    @Test
    void testApplyToMasksCyrillicEntitiesAcrossUnicodeWordBoundaries() {
        // The word-boundary regex uses Pattern.UNICODE_CHARACTER_CLASS so \b recognises non-ASCII letter/digit
        // transitions. Without that flag the Cyrillic e-mail local-part would be treated as non-word and the boundary
        // would fire in the wrong places, causing the mask to no-op on the full e-mail. Pin the Unicode behaviour so a
        // future refactor that drops the flag fails loudly.
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of("александр@пример.рф")));

        String result = map.applyTo("Напишите на александр@пример.рф пожалуйста");

        assertThat(result).isEqualTo("Напишите на <EMAIL> пожалуйста");
    }

    @Test
    void testApplyToMasksValueEmbeddedInRightToLeftText() {
        // RTL embedding characters (U+202B / U+202C) surround the masked value. The boundary-aware pattern must still
        // match because the embedding marks are non-word characters, so the mask is edge-matched without \b on those
        // sides. Guards against a reading-order attack where secrets are injected inside RTL markers to bypass mask
        // regexes that assume ASCII boundaries.
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("TOKEN", List.of("sk-ABCDEFGHIJKLMN")));

        String rtlWrapped = "prefix \u202bsk-ABCDEFGHIJKLMN\u202c suffix";
        String result = map.applyTo(rtlWrapped);

        assertThat(result).isEqualTo("prefix \u202b<TOKEN>\u202c suffix");
    }

    @Test
    void testApplyToDoesNotReMaskInsidePlaceholderWhenLaterEntityValueEqualsTypeLabel() {
        // Regression: the prior implementation ran replaceAll once per entity in length-desc order against the
        // PROGRESSIVELY MUTATED string. After the first replacement turned 'alice@corp.com' into '<EMAIL_ADDRESS>',
        // the second entity whose value is the literal string 'EMAIL_ADDRESS' would match inside that placeholder
        // (the boundary-aware pattern accepts placeholder boundaries because '<' and '>' are non-word chars). The
        // fix collects spans against the original text, so the placeholder is never re-scanned.
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("EMAIL", List.of("alice@corp.com"), "LOC", List.of("EMAIL_ADDRESS")));

        // Only the email is in the input — the literal 'EMAIL_ADDRESS' value is configured but never appears in the
        // source text, so no LOC mask should be inserted. With the old implementation the result would have been
        // 'prefix <EMAIL>' → 'prefix <<LOC>>' (or worse, depending on order), corrupting the text.
        String result = map.applyTo("prefix alice@corp.com suffix");

        assertThat(result).isEqualTo("prefix <EMAIL> suffix");
    }

    @Test
    void testApplyToMasksSecretWithZeroWidthCharsInsideValue() {
        // Zero-width characters (U+200B–U+200D, U+FEFF) inserted INSIDE the entity value must still be masked when
        // the detector emits the exact value-as-seen. This is the realistic detector contract: the upstream detector
        // receives the same content the LLM does, so the masker only needs to find the value verbatim. The check
        // pins that the boundary-aware pattern + Pattern.quote does not accidentally strip ZWJ from the regex.
        String secret = "sk-ABCD\u200BEFGH\u200CIJKL";
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("TOKEN", List.of(secret)));

        String result = map.applyTo("prefix " + secret + " suffix");

        assertThat(result).isEqualTo("prefix <TOKEN> suffix");
    }

    @Test
    void testApplyToHandlesMultiMegabyteInput() {
        // Sanitize-stage input can be very large (long chat transcripts, document attachments). The two-phase
        // scan-then-replace must complete in reasonable time on multi-MB input — pin against an accidental
        // regression where someone reintroduces N replaceAll passes (each O(text)) instead of the single-pass
        // span collection. A 2 MiB string with ~10 entities should mask in well under a second; we don't assert a
        // hard wall-clock to keep the test reliable on slow CI, but we do assert correctness on the boundary
        // (every occurrence is masked, none corrupted).
        StringBuilder builder = new StringBuilder(2 * 1024 * 1024 + 1024);

        for (int i = 0; i < 2 * 1024; i++) {
            builder.append("a-1024-character-block-of-filler-content")
                .append(" - ")
                .append("CARD-1234-5678-9012-3456")
                .append(" - ");
            // Pad to roughly 1 KiB per iteration so total is ~2 MiB.
            builder.append("x".repeat(960));
        }

        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("CARD", List.of("CARD-1234-5678-9012-3456")));

        String masked = map.applyTo(builder.toString());

        assertThat(masked)
            .as("the literal card value must not appear after masking")
            .doesNotContain("CARD-1234-5678-9012-3456");
        assertThat(masked)
            .as("expected one <CARD> per iteration")
            .contains("<CARD>");
    }

    @Test
    void testApplyToMasksValueContainingEmojiSurrogatePair() {
        // U+1F600 (grinning face) is a supplementary character represented as a surrogate pair in UTF-16. The mask
        // regex must treat the pair as a single code point — slicing through the surrogates would either fail to
        // match or produce malformed UTF-16 in the output StringBuilder.
        String value = "Bob \uD83D\uDE00 Smith";
        MaskEntityMap map = new MaskEntityMap();

        map.merge(Map.of("PERSON", List.of(value)));

        String result = map.applyTo("To: " + value + " <bob@x>");

        assertThat(result).isEqualTo("To: <PERSON> <bob@x>");
    }
}

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

package com.bytechef.platform.ai.sensitivedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SensitiveSpanTest {

    @Test
    void testPlaceholderIsDerivedFromCategory() {
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 5)
            .placeholder()).isEqualTo("[REDACTED_EMAIL]");
        assertThat(SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 5)
            .placeholder()).isEqualTo("[REDACTED_SECRET]");
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "PERSON", 0, 5)
            .placeholder()).isEqualTo("[REDACTED_PERSON]");
    }

    @Test
    void testOfDefaultsConfidenceToOne() {
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "IP", 3, 9)
            .confidence()).isEqualTo(1.0);
    }

    @Test
    void testLengthIsEndMinusStart() {
        assertThat(SensitiveSpan.of(SensitiveKind.PII, "IP", 3, 9)
            .length()).isEqualTo(6);
    }

    @Test
    void testOverlapsIsHalfOpenSoTouchingSpansDoNotOverlap() {
        SensitiveSpan first = SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 5);
        SensitiveSpan touching = SensitiveSpan.of(SensitiveKind.PII, "IP", 5, 9);
        SensitiveSpan crossing = SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 9);

        assertThat(first.overlaps(touching)).isFalse();
        assertThat(touching.overlaps(first)).isFalse();
        assertThat(first.overlaps(crossing)).isTrue();
        assertThat(crossing.overlaps(first)).isTrue();
    }

    @Test
    void testRejectsInvalidCategory() {
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "lower", 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "", 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "9LEADING", 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsInvalidRange() {
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "EMAIL", -1, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 5, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 6, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsInvalidConfidence() {
        assertThatThrownBy(() -> new SensitiveSpan(SensitiveKind.PII, "EMAIL", 0, 5, 1.5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SensitiveSpan(SensitiveKind.PII, "EMAIL", 0, 5, Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToStringCarriesNoCoveredText() {
        String rendered = SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 2, 40)
            .toString();

        assertThat(rendered).contains("SECRET");
        assertThat(rendered).contains("2");
        assertThat(rendered).contains("40");
    }

    @Test
    void testWithOffsetMovesBothEndsAndKeepsEverythingElse() {
        SensitiveSpan span = new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 20, 0.9);

        SensitiveSpan offset = span.withOffset(1000);

        assertThat(offset.start()).isEqualTo(1005);
        assertThat(offset.end()).isEqualTo(1020);
        assertThat(offset.kind()).isEqualTo(span.kind());
        assertThat(offset.category()).isEqualTo(span.category());
        assertThat(offset.confidence()).isEqualTo(span.confidence());
    }

    @Test
    void testWithOffsetOfZeroReturnsAnEqualSpan() {
        SensitiveSpan span = new SensitiveSpan(SensitiveKind.SECRET, "AWS_ACCESS_KEY", 0, 20, 0.9);

        assertThat(span.withOffset(0)).isEqualTo(span);
    }
}

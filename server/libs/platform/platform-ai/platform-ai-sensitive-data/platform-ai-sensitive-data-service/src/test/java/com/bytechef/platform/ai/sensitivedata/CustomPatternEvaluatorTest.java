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

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Ivica Cardic
 */
class CustomPatternEvaluatorTest {

    private static final CustomPattern ACME_ID = new CustomPattern(
        "ACME_ACCOUNT_ID", Pattern.compile("\\bACME-\\d{4}-[A-Z]{2}\\b"), SensitiveKind.PII, 0.9, null);

    @Test
    void testAMatchCarriesTheRulesOwnTypeAndScore() {
        List<SensitiveSpan> spans = CustomPatternEvaluator.detect(
            "charge ACME-4417-XY today", List.of(ACME_ID), MatchDeadline.unbounded());

        assertThat(spans).singleElement()
            .satisfies(span -> {
                assertThat(span.category()).isEqualTo("ACME_ACCOUNT_ID");
                assertThat(span.kind()).isEqualTo(SensitiveKind.PII);
                assertThat(span.confidence()).isEqualTo(0.9);
                assertThat(span.start()).isEqualTo(7);
                assertThat(span.end()).isEqualTo(19);
                assertThat("charge ACME-4417-XY today".substring(span.start(), span.end()))
                    .isEqualTo("ACME-4417-XY");
            });
    }

    @Test
    void testAContextRulePromotesACustomMatchTheSameWayItPromotesABuiltInOne() {
        CustomPattern lowScored = new CustomPattern(
            "ACME_SHORT", Pattern.compile("\\b\\d{6}\\b"), SensitiveKind.PII, 0.2,
            new PiiPatternCatalog.ContextRule(Set.of("acme account"), 40, 0.9));

        assertThat(
            CustomPatternEvaluator.detect("acme account 123456", List.of(lowScored), MatchDeadline.unbounded())
                .getFirst()
                .confidence()).isEqualTo(0.9);

        assertThat(
            CustomPatternEvaluator.detect("ticket 123456", List.of(lowScored), MatchDeadline.unbounded())
                .getFirst()
                .confidence()).isEqualTo(0.2);
    }

    @Test
    void testASecretRuleReportsItsOwnKindSoItIsNeverTokenized() {
        CustomPattern secret = new CustomPattern(
            "ACME_SIGNING_KEY", Pattern.compile("\\bak_[A-Za-z0-9]{16}\\b"), SensitiveKind.SECRET, 0.9, null);

        assertThat(
            CustomPatternEvaluator.detect(
                "key ak_abcdefghijklmnop here", List.of(secret), MatchDeadline.unbounded())
                .getFirst()
                .kind()).isEqualTo(SensitiveKind.SECRET);
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testAPathologicalRuleIsInterruptedRatherThanRunOn() {
        CustomPattern runaway = new CustomPattern(
            "ACME_RUNAWAY", Pattern.compile("(x+x+)+y"), SensitiveKind.PII, 0.9, null);

        assertThatThrownBy(
            () -> CustomPatternEvaluator.detect(
                "x".repeat(1000), List.of(runaway), MatchDeadline.in(Duration.ofMillis(100))))
                    .isInstanceOf(DetectionTimeoutException.class);
    }

    @Test
    void testNoPatternsMeansNoWork() {
        assertThat(CustomPatternEvaluator.detect("anything", List.of(), MatchDeadline.unbounded())).isEmpty();
    }

    @Test
    void testEmptyTextYieldsNoSpans() {
        assertThat(CustomPatternEvaluator.detect("", List.of(ACME_ID), MatchDeadline.unbounded())).isEmpty();
    }

    @Test
    void testEveryMatchIsReportedNotJustTheFirst() {
        assertThat(
            CustomPatternEvaluator.detect(
                "ACME-4417-XY and ACME-9902-AB", List.of(ACME_ID), MatchDeadline.unbounded()))
                    .hasSize(2);
    }

    @Test
    void testAContextRuleThatWouldLowerConfidenceIsRejectedAtConstruction() {
        assertThatThrownBy(
            () -> new CustomPattern(
                "ACME_ID", Pattern.compile("\\d{6}"), SensitiveKind.PII, 0.9,
                new PiiPatternCatalog.ContextRule(Set.of("acme"), 40, 0.2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RAISE");
    }
}

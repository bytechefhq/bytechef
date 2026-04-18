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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ViolationTest {

    @Test
    void ofExecutionFailureRecordsException() {
        RuntimeException cause = new RuntimeException("LLM down");

        Violation violation = Violation.ofExecutionFailure("jailbreak", cause);

        assertThat(violation.guardrail()).isEqualTo("jailbreak");
        assertThat(violation).isInstanceOf(Violation.ExecutionFailureViolation.class);

        Violation.ExecutionFailureViolation failure = (Violation.ExecutionFailureViolation) violation;

        assertThat(failure.exception()).isSameAs(cause);
        assertThat(failure.info()).isEmpty();
    }

    @Test
    void ofClassificationProducesClassifiedViolation() {
        Violation violation = Violation.ofClassification("nsfw", 0.9);

        assertThat(violation).isInstanceOf(Violation.ClassifiedViolation.class);
        assertThat(violation).isNotInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void ofExecutionFailureRejectsNullCause() {
        assertThatThrownBy(() -> Violation.ofExecutionFailure("x", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exception");
    }

    @Test
    void ofMatchesRejectsNullListForSymmetryWithOfMatch() {
        // ofMatch(null) already rejects null — ofMatches must too, otherwise an equivalent call through the
        // plural factory silently becomes an empty list and the violation appears to fire with no substrings,
        // an easy-to-misattribute bug for callers that expect the "pass null = no substring" shorthand.
        assertThatThrownBy(() -> Violation.ofMatches("x", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("matchedSubstrings");

        assertThatThrownBy(() -> Violation.ofMatches("x", null, java.util.Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("matchedSubstrings");
    }

    @Test
    void patternViolationCompactConstructorRejectsNullMatchedSubstrings() {
        // Symmetric with the factory — direct record construction must not silently coerce null to an empty list.
        assertThatThrownBy(() -> new Violation.PatternViolation("x", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("matchedSubstrings");
    }

    @Test
    void ofMatchesReturnsAllSubstrings() {
        Violation.PatternViolation violation = (Violation.PatternViolation) Violation.ofMatches(
            "pii", java.util.List.of("a@b.com", "c@d.com"));

        assertThat(violation.matchedSubstrings()).containsExactly("a@b.com", "c@d.com");
    }

    @Test
    void ofMatchesWithInfoPreservesDiagnostics() {
        Violation violation = Violation.ofMatches(
            "urls", java.util.List.of("evil.com"),
            java.util.Map.of(
                "allowed", java.util.List.of("safe.com"),
                "blocked", java.util.List.of("evil.com")));

        assertThat(violation.info())
            .containsEntry("allowed", java.util.List.of("safe.com"))
            .containsEntry("blocked", java.util.List.of("evil.com"));
    }

    @Test
    void ofMatchesWithoutInfoReturnsEmptyMap() {
        Violation violation = Violation.ofMatches("pii", java.util.List.of("a@b.com"));

        assertThat(violation.info()).isEmpty();
    }

    @Test
    void ofMatchReturnsEmptyInfoMap() {
        Violation violation = Violation.ofMatch("pii", "a@b.com");

        assertThat(violation.info()).isEmpty();
    }

    @Test
    void ofClassificationWithInfoPreservesDiagnosticsAndScore() {
        Violation.ClassifiedViolation violation = (Violation.ClassifiedViolation) Violation.ofClassification(
            "nsfw", 0.85, java.util.Map.of("category", "violence"));

        assertThat(violation.info()).containsEntry("category", "violence");
        assertThat(violation.confidenceScore()).isEqualTo(0.85);
    }

    @Test
    void classifiedViolationCarriesScoreOnTheRecord() {
        Violation.ClassifiedViolation violation =
            (Violation.ClassifiedViolation) Violation.ofClassification("nsfw", 0.82);

        assertThat(violation.confidenceScore()).isEqualTo(0.82);
    }

    @Test
    void internalKeysAreStrippedAtConstructionFromInfo() {
        // INTERNAL_INFO_KEYS ("maskEntities") are filtered at record construction, so info() always returns a
        // safe-to-serialize view regardless of what the caller passed in.
        Violation violation = Violation.ofMatches(
            "pii",
            java.util.List.of("a@b.com"),
            java.util.Map.of(
                "entityTypes", java.util.List.of("EMAIL"),
                "maskEntities", java.util.Map.of("EMAIL", java.util.List.of("a@b.com"))));

        assertThat(violation.info())
            .containsEntry("entityTypes", java.util.List.of("EMAIL"))
            .doesNotContainKey("maskEntities");
    }

    @Test
    void infoEmptyWhenNothingPassed() {
        Violation violation = Violation.ofMatch("pii", "a@b.com");

        assertThat(violation.info()).isEmpty();
    }
}

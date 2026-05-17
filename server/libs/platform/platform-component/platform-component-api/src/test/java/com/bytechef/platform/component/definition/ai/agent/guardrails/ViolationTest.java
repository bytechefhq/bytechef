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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        assertThatThrownBy(() -> Violation.ofMatches("x", null, Map.of()))
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
            "pii", List.of("a@b.com", "c@d.com"));

        assertThat(violation.matchedSubstrings()).containsExactly("a@b.com", "c@d.com");
    }

    @Test
    void ofMatchesWithInfoPreservesDiagnostics() {
        Violation violation = Violation.ofMatches(
            "urls", List.of("evil.com"),
            Map.of(
                "allowed", new ArrayList<>(List.of("safe.com")),
                "blocked", new ArrayList<>(List.of("evil.com"))));

        assertThat(violation.info())
            .containsEntry("allowed", new ArrayList<>(List.of("safe.com")))
            .containsEntry("blocked", new ArrayList<>(List.of("evil.com")));
    }

    @Test
    void ofMatchesWithoutInfoReturnsEmptyMap() {
        Violation violation = Violation.ofMatches("pii", List.of("a@b.com"));

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
            "nsfw", 0.85, Map.of("category", "violence"));

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
    void infoIsImmutableDefensiveCopy() {
        // The SPI no longer filters preflight-internal keys (scrubbing moved to the check advisor's toPublicView so
        // the API module doesn't have to know about implementation-internal key names). The compact constructor only
        // takes a defensive copy and rejects mutation; advisor-side scrubbing is pinned by tests in the advisor
        // module (CheckForViolationsAdvisorPublicMetadataTest).
        LinkedHashMap<String, Serializable> source = new LinkedHashMap<>();

        source.put("entityTypes", new ArrayList<>(List.of("EMAIL")));
        source.put("maskEntities", new LinkedHashMap<>(Map.of("EMAIL", new ArrayList<>(List.of("a@b.com")))));

        Violation violation = Violation.ofMatches("pii", List.of("a@b.com"), source);

        assertThat(violation.info()).containsKeys("entityTypes", "maskEntities");

        source.put("entityTypes", new ArrayList<>(List.of("CHANGED")));

        assertThat(violation.info()).containsEntry("entityTypes", new ArrayList<>(List.of("EMAIL")));
    }

    @Test
    void infoEmptyWhenNothingPassed() {
        Violation violation = Violation.ofMatch("pii", "a@b.com");

        assertThat(violation.info()).isEmpty();
    }
}

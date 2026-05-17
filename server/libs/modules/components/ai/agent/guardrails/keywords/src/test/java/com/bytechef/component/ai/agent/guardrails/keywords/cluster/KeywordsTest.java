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

package com.bytechef.component.ai.agent.guardrails.keywords.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class KeywordsTest {

    @Test
    void testMatchesInsensitiveByDefault() throws Exception {
        GuardrailCheckFunction function = resolve();

        Optional<Violation> violation = function.apply(
            "this mentions FORBIDDEN words",
            contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", false)));

        assertThat(violation).isPresent();
        assertThat(violation.get()
            .guardrail()).isEqualTo("keywordsCheck");
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("forbidden");
    }

    @Test
    void testPatternViolationCarriesEveryMatchAndOmitsInfoLeak() throws Exception {
        GuardrailCheckFunction function = resolve();

        Optional<Violation> violation = function.apply(
            "this mentions FORBIDDEN and SECRET words",
            contextOf(Map.of("keywords", List.of("forbidden", "secret"), "caseSensitive", false)));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("forbidden", "secret");
        // Raw keyword values must NOT be duplicated into info: the advisor's public-view projection reduces
        // matchedSubstrings to matchCount, but copies info verbatim, so any raw value placed in info would bypass the
        // scrubbing. Keep this assertion so a future "convenience" info key on keywords does not regress the invariant.
        assertThat(violation.get()
            .info()).doesNotContainKey("matchedKeywords");
    }

    @Test
    void testNoMatch() throws Exception {
        GuardrailCheckFunction function = resolve();

        Optional<Violation> violation = function.apply(
            "clean text",
            contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", false)));

        assertThat(violation).isEmpty();
    }

    @Test
    void testEmptyKeywordListFailsClosed() {
        GuardrailCheckFunction function = resolve();

        // A misconfigured workflow with an empty or missing keyword list must NOT silently report "no match".
        // The advisor wraps the IllegalArgumentException into a blocking ExecutionFailureViolation; if this ever
        // reverts to "no match", every keyword guardrail with an empty list silently fails open.
        assertThatThrownBy(
            () -> function.apply("anything", contextOf(Map.of("keywords", List.of(), "caseSensitive", false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one entry");
    }

    @Test
    void testMissingKeywordListFailsClosed() {
        GuardrailCheckFunction function = resolve();

        assertThatThrownBy(
            () -> function.apply("anything", contextOf(Map.of("caseSensitive", false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one entry");
    }

    @Test
    void testKeywordCheckRunsInPreflightStageOnRawText() throws Exception {
        GuardrailCheckFunction function = resolve();

        // Keywords MUST run at PREFLIGHT, before sibling masking detectors (SecretKeys, PII, URLs) rewrite tokens.
        // Otherwise a deny-list keyword like "AKIA" never matches because SecretKeys preflight already masked it.
        assertThat(function.stage()).isEqualTo(GuardrailStage.PREFLIGHT);
        assertThat(function.requiresChatClient()).isFalse();
    }

    @Test
    void testSanitizeMasksMatchedKeywords() throws Exception {
        GuardrailSanitizerFunction sanitize = resolveSanitize();

        String masked = sanitize.apply(
            "this mentions FORBIDDEN words",
            contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", false)));

        assertThat(masked).isEqualTo("this mentions <KEYWORD> words");
    }

    @Test
    void testSanitizeMaskResultProducesEntities() {
        PreflightSanitizerFunction sanitize = (PreflightSanitizerFunction) resolveSanitize();

        MaskResult result = sanitize.mask(
            "FORBIDDEN and SECRET in the same text",
            contextOf(Map.of("keywords", List.of("forbidden", "secret"), "caseSensitive", false)));

        assertThat(result).isInstanceOf(MaskResult.Entities.class);

        MaskResult.Entities entities = (MaskResult.Entities) result;
        Map<String, List<String>> entityMap = entities.entities();

        assertThat(entityMap).containsKey("KEYWORD");
        assertThat(entityMap.get("KEYWORD")).containsExactlyInAnyOrder("FORBIDDEN", "SECRET");
    }

    @Test
    void testSanitizeStageIsPreflight() {
        GuardrailSanitizerFunction sanitize = resolveSanitize();

        assertThat(sanitize.stage()).isEqualTo(GuardrailStage.PREFLIGHT);
        assertThat(sanitize.requiresChatClient()).isFalse();
    }

    @Test
    void testSanitizeNoMatchLeavesTextUntouched() throws Exception {
        GuardrailSanitizerFunction sanitize = resolveSanitize();

        String unchanged = sanitize.apply(
            "no deny-list tokens here",
            contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", false)));

        assertThat(unchanged).isEqualTo("no deny-list tokens here");
    }

    @Test
    void testSanitizeMaskResultEmptyWhenNoMatch() {
        PreflightSanitizerFunction sanitize = (PreflightSanitizerFunction) resolveSanitize();

        MaskResult result = sanitize.mask(
            "no deny-list tokens here",
            contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", false)));

        assertThat(result).isInstanceOf(MaskResult.Unchanged.class);
    }

    @Test
    void testSanitizeRespectsCaseSensitivity() throws Exception {
        GuardrailSanitizerFunction sanitize = resolveSanitize();

        String caseSensitive = sanitize.apply(
            "FORBIDDEN and forbidden",
            contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", true)));

        assertThat(caseSensitive).isEqualTo("FORBIDDEN and <KEYWORD>");
    }

    @Test
    void testSanitizeEmptyKeywordListFailsClosed() {
        GuardrailSanitizerFunction sanitize = resolveSanitize();

        assertThatThrownBy(
            () -> sanitize.apply("anything", contextOf(Map.of("keywords", List.of(), "caseSensitive", false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one entry");
    }

    @Test
    void testSanitizeNullOrEmptyTextReturnsAsIs() throws Exception {
        GuardrailSanitizerFunction sanitize = resolveSanitize();

        GuardrailContext context = contextOf(Map.of("keywords", List.of("forbidden"), "caseSensitive", false));

        assertThat(sanitize.apply("", context)).isEqualTo("");
        assertThat(sanitize.apply(null, context)).isNull();
    }

    private static GuardrailCheckFunction resolve() {
        ClusterElementDefinition<GuardrailCheckFunction> def = Keywords.ofCheck();

        return def.getElement();
    }

    private static GuardrailSanitizerFunction resolveSanitize() {
        ClusterElementDefinition<GuardrailSanitizerFunction> def = Keywords.ofSanitize();

        return def.getElement();
    }

    private static GuardrailContext contextOf(Map<String, ?> input) {
        return new GuardrailContext(
            ParametersFactory.create(input),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            null,
            mock(Context.class));
    }
}

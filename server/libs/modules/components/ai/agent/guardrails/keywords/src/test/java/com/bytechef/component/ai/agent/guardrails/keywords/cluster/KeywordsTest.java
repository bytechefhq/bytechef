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

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
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
            .guardrail()).isEqualTo("keywords");
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
    void testKeywordCheckRunsInLlmStageOnMaskedText() throws Exception {
        GuardrailCheckFunction function = resolve();

        assertThat(function.stage()).isEqualTo(GuardrailStage.LLM);

        // Upstream masking replaces `alice` inside the e-mail with <EMAIL>; the keyword check then sees the masked
        // text and must not fire on the token `alice` that has been replaced.
        Optional<Violation> violation = function.apply(
            "<EMAIL> said something",
            contextOf(Map.of("keywords", List.of("alice"), "caseSensitive", false)));

        assertThat(violation).isEmpty();
    }

    private static GuardrailCheckFunction resolve() {
        ClusterElementDefinition<GuardrailCheckFunction> def = Keywords.of();

        return def.getElement();
    }

    private static GuardrailContext contextOf(Map<String, ?> input) {
        return new GuardrailContext(
            ParametersFactory.create(input),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of());
    }
}

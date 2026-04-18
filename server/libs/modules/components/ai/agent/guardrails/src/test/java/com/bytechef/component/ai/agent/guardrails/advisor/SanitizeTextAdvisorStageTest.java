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

package com.bytechef.component.ai.agent.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorStageTest {

    @Test
    void preflightSanitizersRunBeforeLlmSanitizers() {
        List<String> textsSeenByLlmSanitizer = new ArrayList<>();

        GuardrailSanitizerFunction piiMask = new GuardrailSanitizerFunction() {

            @Override
            public String apply(String text, GuardrailContext context) {
                return text.replace("a@b.com", "<EMAIL>");
            }
            // default stage is PREFLIGHT
        };

        GuardrailSanitizerFunction llmRewrite = new GuardrailSanitizerFunction() {

            @Override
            public String apply(String text, GuardrailContext context) {
                textsSeenByLlmSanitizer.add(text);

                return text;
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("pii", piiMask, empty, empty, empty, Map.of(), null)
            .add("llmRewrite", llmRewrite, empty, empty, empty, Map.of(), null)
            .build();

        String result = advisor.sanitiseForTesting("reach me at a@b.com");

        assertThat(textsSeenByLlmSanitizer).containsExactly("reach me at <EMAIL>");
        assertThat(result).contains("<EMAIL>");
    }

    @Test
    void failingSanitizerRunsOtherSanitizersBeforeAggregateThrow() {
        GuardrailSanitizerFunction broken = new GuardrailSanitizerFunction() {

            @Override
            public String apply(String text, GuardrailContext context) {
                throw new RuntimeException("boom");
            }
        };

        GuardrailSanitizerFunction working = new GuardrailSanitizerFunction() {

            @Override
            public String apply(String text, GuardrailContext context) {
                return text.replace("hello", "<HELLO>");
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("broken", broken, empty, empty, empty, Map.of(), null)
            .add("working", working, empty, empty, empty, Map.of(), null)
            .build();

        // The advisor collects all failures and throws a SanitizerExecutionFailureException at the end; sibling
        // sanitizers still run against the last known-good intermediate text. The headline contains the sanitizer
        // name + exception class name — raw cause messages (which may include user prompt fragments) are exposed
        // only via the cause chain, never in the headline that every logger prints.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> advisor.sanitiseForTesting("hello world"))
            .isInstanceOf(
                com.bytechef.component.ai.agent.guardrails.SanitizerExecutionFailureException.class)
            .hasMessageContaining("broken - RuntimeException")
            .hasMessageNotContaining("boom")
            .hasRootCauseMessage("boom");
    }
}

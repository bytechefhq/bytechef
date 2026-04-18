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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ai.agent.guardrails.SanitizerExecutionFailureException;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorAggregateErrorTest {

    @Test
    void multipleSanitizerFailuresAggregateIntoSingleThrow() {
        GuardrailSanitizerFunction broken1 = (text, context) -> {
            throw new RuntimeException("LLM down");
        };
        GuardrailSanitizerFunction broken2 = (text, context) -> {
            throw new RuntimeException("bad regex");
        };

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("llmPii", broken1, empty, empty, empty, Map.of(), null)
            .add("customRegex", broken2, empty, empty, empty, Map.of(), null)
            .build();

        // Headline contains sanitizer name + exception class, not raw cause message (which could leak user content).
        // Operators wanting the raw cause go to the populated getCause() / getSuppressed() chain.
        assertThatThrownBy(() -> advisor.sanitiseForTesting("hello"))
            .isInstanceOf(SanitizerExecutionFailureException.class)
            .hasMessageContaining("llmPii - RuntimeException")
            .hasMessageContaining("customRegex - RuntimeException")
            .hasMessageNotContaining("LLM down")
            .hasMessageNotContaining("bad regex")
            .hasRootCauseMessage("LLM down");
    }

    @Test
    void singleSuccessfulSanitizerDoesNotThrow() {
        GuardrailSanitizerFunction piiMask = (text, context) -> text.replace("a@b.com", "<EMAIL>");

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("pii", piiMask, empty, empty, empty, Map.of(), null)
            .build();

        String result = advisor.sanitiseForTesting("reach me at a@b.com");

        assertThat(result).isEqualTo("reach me at <EMAIL>");
    }

    @Test
    void failuresCarryOnOriginalExceptions() {
        RuntimeException cause = new IllegalStateException("bad state");
        GuardrailSanitizerFunction broken = (text, context) -> {
            throw cause;
        };

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("s", broken, empty, empty, empty, Map.of(), null)
            .build();

        assertThatThrownBy(() -> advisor.sanitiseForTesting("x"))
            .isInstanceOfSatisfying(SanitizerExecutionFailureException.class, exception -> assertThat(
                exception.failures()).containsEntry("s", cause));
    }
}

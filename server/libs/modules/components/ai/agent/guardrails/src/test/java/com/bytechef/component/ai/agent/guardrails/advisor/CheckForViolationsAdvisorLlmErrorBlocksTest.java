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

import com.bytechef.component.ai.agent.guardrails.GuardrailOutputParseException;
import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.definition.Parameters;
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
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Regression pin: a fail-closed LLM check that throws MUST block the request. If this test fails, either the default
 * fail mode or the advisor's exception aggregation has regressed and LLM outages are silently letting traffic through —
 * a major security regression.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorLlmErrorBlocksTest {

    @Test
    void failClosedLlmErrorBlocksRequestAndRecordsExecutionFailure() {
        GuardrailCheckFunction brokenLlm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                throw new GuardrailUnavailableException("jailbreak", "LLM down", new RuntimeException());
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", brokenLlm, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> {
                assertThat(violation.guardrail()).isEqualTo("jailbreak");
                assertThat(violation).isInstanceOf(Violation.ExecutionFailureViolation.class);
                assertThat(((Violation.ExecutionFailureViolation) violation).exception())
                    .isInstanceOf(GuardrailUnavailableException.class);
            });
    }

    @Test
    void failClosedOutputParseErrorBlocksRequestAndRecordsExecutionFailure() {
        // Regression pin: if isConfigurationError is refactored to exclude GuardrailOutputParseException, a malformed
        // LLM response would silently pass through. The advisor must treat the parse failure as fail-closed — it's a
        // prompt/schema bug on our side, not a transient outage, and operators need to see the request blocked while
        // they fix the prompt.
        GuardrailCheckFunction parseFailingLlm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                throw new GuardrailOutputParseException(
                    "jailbreak", "schema drift: missing flagged field", new RuntimeException("parse"));
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        // Use FAIL_OPEN to verify the advisor treats output-parse as fail-closed regardless — that's the whole point
        // of the isConfigurationError whitelist: parse failures originate from prompt/schema bugs and will not recover
        // from retry, so they must always block even when the operator has chosen fail-open for transient outages.
        Parameters failOpen = ParametersFactory.create(Map.of("failMode", "FAIL_OPEN"));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", parseFailingLlm, failOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> {
                assertThat(violation).isInstanceOf(Violation.ExecutionFailureViolation.class);

                Violation.ExecutionFailureViolation failure = (Violation.ExecutionFailureViolation) violation;

                assertThat(failure.guardrail()).isEqualTo("jailbreak");
                assertThat(failure.exception()).isInstanceOf(GuardrailOutputParseException.class);
            });
    }
}

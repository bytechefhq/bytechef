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
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorPreflightTest {

    @Test
    void llmStageChecksSeePreflightMaskedText() {
        List<String> textsSeenByLlmCheck = new ArrayList<>();

        PreflightCheckFunction maskingPreflight = new PreflightCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.of(Violation.ofMatch("pii", "a@b.com"));
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.masked(text.replace("a@b.com", "<EMAIL>"), text);
            }
        };

        GuardrailCheckFunction llmCheck = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                textsSeenByLlmCheck.add(text);

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", maskingPreflight, empty, empty, empty, empty, Map.of(), null)
            .add("jailbreak", llmCheck, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("reach me at a@b.com")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)
            .guardrail()).isEqualTo("pii");
        assertThat(textsSeenByLlmCheck).containsExactly("reach me at <EMAIL>");
    }

    @Test
    void llmStageStillSeesOriginalTextWhenNoPreflightMaskFires() {
        List<String> textsSeenByLlmCheck = new ArrayList<>();

        GuardrailCheckFunction nonMaskingPreflight = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.empty();
            }
            // Not PreflightMasking — default dispatch falls through without any mask() call.
        };

        GuardrailCheckFunction llmCheck = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                textsSeenByLlmCheck.add(text);

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", nonMaskingPreflight, empty, empty, empty, empty, Map.of(), null)
            .add("jailbreak", llmCheck, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello world")))
            .build();

        advisor.runChecksForTesting(request);

        assertThat(textsSeenByLlmCheck).containsExactly("hello world");
    }
}

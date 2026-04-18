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

/**
 * Pins overlap-masking: when multiple preflight checks declare {@code preflightMaskEntities}, the advisor merges them,
 * sorts by length descending, and applies mask replacement once globally. The longer match always wins regardless of
 * cluster registration order.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorOverlapMaskTest {

    @Test
    void longerOverlappingMatchIsAppliedBeforeShorter() {
        PreflightCheckFunction pii = new PreflightCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.of(Violation.ofMatch("pii", "alice@corp.com"));
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.entities(Map.of("EMAIL", List.of("alice@corp.com")));
            }
        };

        PreflightCheckFunction urls = new PreflightCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.of(Violation.ofMatch("urls", "corp.com"));
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.entities(Map.of("URL", List.of("corp.com")));
            }
        };

        List<String> textsSeenByLlm = new ArrayList<>();
        GuardrailCheckFunction llm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                textsSeenByLlm.add(text);

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("x")
            // Register URLs first to prove order-of-registration does not matter for the masking outcome.
            .add("urls", urls, empty, empty, empty, empty, Map.of(), null)
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .add("llm", llm, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("ping alice@corp.com now")))
            .build();

        advisor.runChecksForTesting(request);

        // The email match wins — longer-first global replacement — so the LLM stage never sees the raw local-part.
        assertThat(textsSeenByLlm).containsExactly("ping <EMAIL> now");
    }
}

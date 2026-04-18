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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CUSTOMIZE_SYSTEM_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SYSTEM_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
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
 * Regression pin: the parent {@code CheckForViolations} is the single owner of {@code systemMessage}, and every LLM
 * child receives the same value via {@code GuardrailContext.parentParameters()}. If a future refactor moves
 * {@code systemMessage} onto individual LLM cluster elements, this test fails and the divergence is caught before it
 * ships. The test uses lightweight stand-in check functions — instantiating each real LLM cluster element would cross
 * Gradle module boundaries this module cannot see.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsSharedSystemMessageTest {

    @Test
    void parentSystemMessageIsVisibleToEveryLlmChildViaGuardrailContext() {
        List<String> systemMessagesSeen = new ArrayList<>();

        GuardrailCheckFunction capturing = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                Parameters parent = context.parentParameters();

                if (parent.getBoolean(CUSTOMIZE_SYSTEM_MESSAGE, false)) {
                    systemMessagesSeen.add(parent.getString(SYSTEM_MESSAGE));
                }

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());
        Parameters parent = ParametersFactory.create(Map.of(
            CUSTOMIZE_SYSTEM_MESSAGE, true,
            SYSTEM_MESSAGE, "My org: refuse topics related to hacking"));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", capturing, empty, empty, parent, empty, Map.of(), null)
            .add("nsfw", capturing, empty, empty, parent, empty, Map.of(), null)
            .add("topicalAlignment", capturing, empty, empty, parent, empty, Map.of(), null)
            .add("custom", capturing, empty, empty, parent, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello")))
            .build();

        advisor.runChecksForTesting(request);

        assertThat(systemMessagesSeen)
            .hasSize(4)
            .allMatch(text -> text.equals("My org: refuse topics related to hacking"));
    }
}

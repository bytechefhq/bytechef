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
import static org.mockito.Mockito.mock;

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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pins the shared-MODEL resolution contract: a single {@link ChatClient} passed to
 * {@link CheckForViolationsAdvisor.Builder#add} for multiple LLM children is injected into every child's
 * {@link GuardrailContext#chatClient()}. If a future refactor introduces per-child MODEL resolution or clones the
 * ChatClient, this test catches it.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorSharedModelTest {

    @Test
    void sameChatClientIsInjectedIntoEveryLlmChildContext() {
        ChatClient sharedChatClient = mock(ChatClient.class);

        List<ChatClient> clientsSeen = new ArrayList<>();

        GuardrailCheckFunction llmChild = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                clientsSeen.add(context.chatClient()
                    .orElse(null));

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
            .add("jailbreak", llmChild, empty, empty, empty, empty, Map.of(), sharedChatClient)
            .add("nsfw", llmChild, empty, empty, empty, empty, Map.of(), sharedChatClient)
            .add("topicalAlignment", llmChild, empty, empty, empty, empty, Map.of(), sharedChatClient)
            .add("custom", llmChild, empty, empty, empty, empty, Map.of(), sharedChatClient)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello")))
            .build();

        advisor.runChecksForTesting(request);

        assertThat(clientsSeen)
            .hasSize(4)
            .allSatisfy(seen -> assertThat(seen).isSameAs(sharedChatClient));
    }

    @Test
    void nullChatClientIsPreservedAcrossChildrenWhenModelIsAbsent() {
        List<ChatClient> clientsSeen = new ArrayList<>();

        GuardrailCheckFunction llmChild = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                clientsSeen.add(context.chatClient()
                    .orElse(null));

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
            .add("jailbreak", llmChild, empty, empty, empty, empty, Map.of(), null)
            .add("nsfw", llmChild, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello")))
            .build();

        advisor.runChecksForTesting(request);

        assertThat(clientsSeen)
            .hasSize(2)
            .allSatisfy(seen -> assertThat(seen).isNull());
    }
}

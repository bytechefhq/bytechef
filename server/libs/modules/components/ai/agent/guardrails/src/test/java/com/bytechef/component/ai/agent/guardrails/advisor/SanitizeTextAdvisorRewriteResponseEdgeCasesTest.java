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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pins the two edge cases in {@code SanitizeTextAdvisor.rewriteResponse} that previously had no explicit coverage:
 * <ul>
 * <li>Null or empty {@code chatResponse} upstream → fail-closed, produce the withheld-placeholder. The advisor cannot
 * prove what the model produced was safe, so passing through would leak potentially-unredacted text.</li>
 * <li>Tool-call-only generation with {@code null} text → pass through untouched with an ERROR log. Tool-call payloads
 * are documented as NOT sanitized here; withholding would break every tool-using agent.</li>
 * </ul>
 * A refactor that swapped either branch (e.g. made the null-chatResponse branch pass-through, or made the null-text
 * branch withhold) would be a silent behaviour change without these pins.
 */
class SanitizeTextAdvisorRewriteResponseEdgeCasesTest {

    @Test
    void nullChatResponseFromUpstreamTriggersWithheldPlaceholder() {
        GuardrailSanitizerFunction passThrough = (text, context) -> text;

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", passThrough, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hi");

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(
            ChatClientResponse.builder()
                .chatResponse(null)
                .build());

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("null chatResponse ⇒ we cannot prove safety, so the withheld-placeholder must replace it")
                .contains("sanitizer failed");
    }

    @Test
    void emptyGenerationsFromUpstreamTriggersWithheldPlaceholder() {
        GuardrailSanitizerFunction passThrough = (text, context) -> text;

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", passThrough, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hi");

        ChatResponse empty = ChatResponse.builder()
            .generations(List.of())
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(
            ChatClientResponse.builder()
                .chatResponse(empty)
                .build());

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("empty generations ⇒ advisor withholds, same as null chatResponse")
                .contains("sanitizer failed");
    }

    @Test
    void nullTextGenerationFromToolCallIsPassedThroughUntouched() {
        // A sanitizer that would rewrite text if it ran; we want to prove it does NOT run on the null-text generation
        // so tool-call-only messages survive the advisor untouched. A refactor that throws on null text (or withholds)
        // would break every tool-using agent the sanitizer is placed in front of.
        GuardrailSanitizerFunction wouldRewrite = (text, context) -> {
            throw new AssertionError("sanitize must not be invoked for null-text generations");
        };

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("wouldRewrite", wouldRewrite, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hi");

        AssistantMessage toolCallOnly = AssistantMessage.builder()
            .content(null)
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(toolCallOnly)))
            .build();

        ChatClientResponse upstream = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(upstream);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("null text ⇒ generation must pass through unchanged (tool-calls / media preserved for downstream)")
                .isNull();
    }

    private static ChatClientRequest requestWithUser(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(text)))
            .build();
    }
}

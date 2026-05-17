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

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * @author Ivica Cardic
 */
class SanitizeTextAdvisorRewriteResponseEdgeCasesTest {

    @Test
    void nullChatResponseFromUpstreamIsForwardedUnchanged() {
        // After the simplification, a null chatResponse is no longer fail-closed — the sanitizer returns the
        // response unchanged. Pins this so a future "fail-closed on null response" regression is caught.
        GuardrailSanitizerFunction passThrough = (text, context) -> text;

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", passThrough, null, null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse upstream = ChatClientResponse.builder()
            .chatResponse(null)
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(upstream);

        ChatClientResponse response = advisor.adviseCall(requestWithUser("hi"), chain);

        assertThat(response)
            .as("null chatResponse forwards through rewriteResponse unchanged")
            .isSameAs(upstream);
    }

    @Test
    void nullTextGenerationFromToolCallIsPassedThroughUntouched() {
        // A sanitizer that would rewrite text if it ran; we want to prove it does NOT run on the null-text generation
        // so tool-call-only messages survive the advisor untouched. A refactor that throws on null text (or withholds)
        // would break every tool-using agent the sanitizer is placed in front of.
        List<String> seenTexts = new CopyOnWriteArrayList<>();
        GuardrailSanitizerFunction recordingSanitizer = (text, context) -> {
            seenTexts.add(text);

            return text;
        };

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("recordingSanitizer", recordingSanitizer, null, null)
            .context(mock(Context.class))
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
        // The sanitizer should have seen the user input pass only — never the (null) tool-call output, because
        // rewriteResponse short-circuits null-text generations before invoking sanitize.
        assertThat(seenTexts)
            .as("recording sanitizer should run on input 'hi' only, never on the null-text generation")
            .containsExactly("hi");
    }

    private static ChatClientRequest requestWithUser(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(text)))
            .build();
    }
}

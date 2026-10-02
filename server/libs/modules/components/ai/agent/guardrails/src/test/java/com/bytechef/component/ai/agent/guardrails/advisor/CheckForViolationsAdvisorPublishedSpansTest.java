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
import com.bytechef.platform.ai.guardrails.PublishedInputSpans;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
class CheckForViolationsAdvisorPublishedSpansTest {

    private static final SensitiveSpan PUBLISHED = SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 7);

    @Test
    void testPreflightInputChecksReceiveThePublishedSpans() {
        AtomicReference<List<SensitiveSpan>> seenOnInput = new AtomicReference<>();
        AtomicReference<List<SensitiveSpan>> seenOnOutput = new AtomicReference<>();

        PreflightCheckFunction recording = (text, context) -> {
            if ("[PII_EMAIL_ADDRESS_1_abcd]".equals(text)) {
                seenOnInput.set(context.publishedInputSpans());
            } else {
                seenOnOutput.set(context.publishedInputSpans());
            }

            return Optional.empty();
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .context(mock(Context.class))
            .add("recording", recording, ParametersFactory.create(Map.of("validateOutput", true)),
                ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(
            ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("model text")))))
                .build());

        advisor.adviseCall(
            ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new UserMessage("[PII_EMAIL_ADDRESS_1_abcd]"))))
                .context(PublishedInputSpans.CONTEXT_KEY, List.of(PUBLISHED))
                .build(),
            chain);

        assertThat(seenOnInput.get()).containsExactly(PUBLISHED);
        assertThat(seenOnOutput.get())
            .as("published spans describe the caller's input; an output check judges the model's contribution only")
            .isEmpty();
    }

    @Test
    void testASpanViolationBlocksAndReportsItsCountInThePublicView() {
        PreflightCheckFunction fromSpans = (text, context) -> context.publishedInputSpans()
            .isEmpty()
                ? Optional.empty()
                : Optional.of(Violation.ofSpans("piiCheck", context.publishedInputSpans()
                    .size(), Map.of()));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .context(mock(Context.class))
            .add("piiCheck", fromSpans, ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
                ParametersFactory.create(Map.of()))
            .build();

        ChatClientResponse response = advisor.adviseCall(
            ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new UserMessage("[PII_EMAIL_ADDRESS_1_abcd]"))))
                .context(PublishedInputSpans.CONTEXT_KEY, List.of(PUBLISHED))
                .build(),
            mock(CallAdvisorChain.class));

        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) chatResponse.getMetadata()
            .get(CheckForViolationsAdvisor.VIOLATIONS_METADATA_KEY);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst()).containsEntry("guardrail", "piiCheck")
            .containsEntry("matchCount", 1);
    }
}

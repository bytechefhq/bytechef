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

package com.bytechef.component.ai.agent.guardrails.llmpii.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmPiiDetector;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

@ExtendWith(ObjectMapperSetupExtension.class)
class LlmPiiTest {

    @Test
    void testCheckProducesViolationFromDetectedSpans() throws Exception {
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of(
            new LlmPiiDetector.Span("EMAIL", "alice@example.com"))));

        GuardrailCheckFunction function = (GuardrailCheckFunction) LlmPii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "contact alice@example.com please",
            contextOf(Map.of("entities", List.of("EMAIL")), chatClient));

        assertThat(violation).isPresent();
        assertThat(((Violation.PatternViolation) violation.get()).matchedSubstrings())
            .containsExactly("alice@example.com");
        assertThat(violation.get()
            .guardrail()).isEqualTo("llmPiiCheck");
    }

    @Test
    void testCheckEmptyWhenNoSpans() throws Exception {
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of()));

        GuardrailCheckFunction function = (GuardrailCheckFunction) LlmPii.ofCheck()
            .getElement();

        Optional<Violation> violation = function.apply(
            "nothing sensitive here",
            contextOf(Map.of("entities", List.of("EMAIL")), chatClient));

        assertThat(violation).isEmpty();
    }

    @Test
    void testSanitizeReplacesDetectedSpans() throws Exception {
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of(
            new LlmPiiDetector.Span("EMAIL", "alice@example.com"))));

        GuardrailSanitizerFunction function = (GuardrailSanitizerFunction) LlmPii.ofSanitize()
            .getElement();

        String sanitized = function.apply(
            "contact alice@example.com please",
            contextOf(Map.of("entities", List.of("EMAIL")), chatClient));

        assertThat(sanitized).isEqualTo("contact <EMAIL> please");
    }

    @Test
    void testSanitizeMasksLongestSpanFirstWhenInnerSubstringOverlaps() throws Exception {
        // LLM returns the inner "John" before the outer "John Smith". Without longest-first ordering the short span
        // would mask first and the full-name span would never match, leaving "<PERSON> Smith" instead of "<PERSON>".
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of(
            new LlmPiiDetector.Span("PERSON", "John"),
            new LlmPiiDetector.Span("PERSON", "John Smith"))));

        GuardrailSanitizerFunction function = (GuardrailSanitizerFunction) LlmPii.ofSanitize()
            .getElement();

        String sanitized = function.apply(
            "John Smith said hello",
            contextOf(Map.of("entities", List.of("PERSON")), chatClient));

        assertThat(sanitized).isEqualTo("<PERSON> said hello");
    }

    @Test
    void testCheckThrowsMissingModelChildExceptionWhenContextChatClientIsNull() {
        GuardrailCheckFunction function = (GuardrailCheckFunction) LlmPii.ofCheck()
            .getElement();

        assertThatThrownBy(() -> function.apply("hello", contextOf(Map.of(), null)))
            .isInstanceOf(MissingModelChildException.class)
            .hasMessageContaining("llmPiiCheck");
    }

    @Test
    void testSanitizeThrowsMissingModelChildExceptionWhenContextChatClientIsNull() {
        GuardrailSanitizerFunction function = (GuardrailSanitizerFunction) LlmPii.ofSanitize()
            .getElement();

        assertThatThrownBy(() -> function.apply("hello", contextOf(Map.of(), null)))
            .isInstanceOf(MissingModelChildException.class)
            .hasMessageContaining("llmPiiSanitize");
    }

    @Test
    void testStagesAreLlm() {
        GuardrailCheckFunction check = (GuardrailCheckFunction) LlmPii.ofCheck()
            .getElement();
        GuardrailSanitizerFunction sanitize = (GuardrailSanitizerFunction) LlmPii.ofSanitize()
            .getElement();

        assertThat(check.stage()).isEqualTo(GuardrailStage.LLM);
        assertThat(sanitize.stage()).isEqualTo(GuardrailStage.LLM);
    }

    private static GuardrailContext contextOf(Map<String, ?> input, ChatClient chatClient) {
        Parameters empty = ParametersFactory.create(Map.of());

        return new GuardrailContext(
            ParametersFactory.create(input),
            empty,
            empty,
            empty,
            Map.of(),
            chatClient);
    }

    private static ChatClient mockClient(LlmPiiDetector.Response response) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenReturn(response);

        return chatClient;
    }
}

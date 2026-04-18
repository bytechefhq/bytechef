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

package com.bytechef.component.ai.agent.guardrails.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.GuardrailOutputParseException;
import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import tools.jackson.core.JacksonException;

class LlmPiiDetectorTest {

    private static final String GUARDRAIL = "llmPii";

    @Test
    void testDetectReturnsSpansParsedFromLlmEntityResponse() {
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of(
            new LlmPiiDetector.Span("EMAIL", "alice@example.com"),
            new LlmPiiDetector.Span("PERSON", "Alice Adams"))));

        List<LlmPiiDetector.Span> spans = LlmPiiDetector.detect(
            GUARDRAIL, chatClient, "Contact Alice Adams at alice@example.com",
            List.of("EMAIL", "PERSON", "PHONE"));

        assertThat(spans).hasSize(2);
        assertThat(spans)
            .extracting(LlmPiiDetector.Span::value)
            .containsExactly("alice@example.com", "Alice Adams");
    }

    @Test
    void testDropsHallucinatedSpansNotPresentInInput() {
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of(
            new LlmPiiDetector.Span("EMAIL", "alice@example.com"),
            new LlmPiiDetector.Span("EMAIL", "bob@elsewhere.org"))));

        List<LlmPiiDetector.Span> spans = LlmPiiDetector.detect(
            GUARDRAIL, chatClient, "Contact alice@example.com", List.of("EMAIL"));

        assertThat(spans)
            .extracting(LlmPiiDetector.Span::value)
            .containsExactly("alice@example.com");
    }

    @Test
    void testAllHallucinatedSpansFailClosedWithGuardrailUnavailableException() {
        // Regression: when 100% of returned spans fail the substring guard, the model is degraded. Returning
        // List.of() (the prior behaviour) would silently let the prompt through unmasked while only signalling the
        // failure via a WARN log — the advisor must instead see a thrown exception so it can fail closed.
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(List.of(
            new LlmPiiDetector.Span("EMAIL", "ghost1@nowhere.org"),
            new LlmPiiDetector.Span("EMAIL", "ghost2@nowhere.org"))));

        assertThatThrownBy(() -> LlmPiiDetector.detect(
            GUARDRAIL, chatClient, "Contact alice@example.com", List.of("EMAIL")))
                .isInstanceOf(GuardrailUnavailableException.class)
                .hasMessageContaining("100% hallucination");
    }

    @Test
    void testEmptyInputReturnsEmpty() {
        ChatClient chatClient = mock(ChatClient.class);

        assertThat(LlmPiiDetector.detect(GUARDRAIL, chatClient, "", List.of("EMAIL"))).isEmpty();
        assertThat(LlmPiiDetector.detect(GUARDRAIL, chatClient, null, List.of("EMAIL"))).isEmpty();
    }

    @Test
    void testNullRequestedTypesFailsClosedWithConfigurationError() {
        // Prevent the silent no-op regression: an LLM PII guardrail with no entity types forwarded would see the
        // model produce an empty response (the system prompt says "only include types from the list"), the
        // detector would return empty, and every PII value would flow through unmasked with no error. The
        // detector now raises IllegalArgumentException; the advisor's isConfigurationError treats this as
        // fail-closed regardless of fail mode.
        ChatClient chatClient = mock(ChatClient.class);

        assertThatThrownBy(() -> LlmPiiDetector.detect(GUARDRAIL, chatClient, "alice@example.com", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no requested entity types");
    }

    @Test
    void testEmptyRequestedTypesFailsClosedWithConfigurationError() {
        // Companion of testNullRequestedTypesFailsClosedWithConfigurationError — empty list is the same silent
        // no-op risk as null and gets the same IllegalArgumentException treatment.
        ChatClient chatClient = mock(ChatClient.class);

        assertThatThrownBy(() -> LlmPiiDetector.detect(GUARDRAIL, chatClient, "anything", List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no requested entity types");
    }

    @Test
    void testNullParsedResponseFailsClosedWithGuardrailUnavailableException() {
        // Mirrors LlmClassifier.classify — a null-parsed Response is an upstream bug, not "no PII found".
        ChatClient chatClient = mockClient(null);

        assertThatThrownBy(() -> LlmPiiDetector.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL")))
            .isInstanceOf(GuardrailUnavailableException.class)
            .hasMessageContaining(GUARDRAIL);
    }

    @Test
    void testJacksonExceptionSurfacesAsGuardrailOutputParseException() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenThrow(new TestJacksonException("unexpected token"));

        assertThatThrownBy(() -> LlmPiiDetector.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL")))
            .isInstanceOf(GuardrailOutputParseException.class)
            .hasMessageContaining(GUARDRAIL)
            .hasMessageContaining("Failed to parse");
    }

    /** Subclass needed because {@link JacksonException} constructors are protected. */
    private static final class TestJacksonException extends JacksonException {
        TestJacksonException(String message) {
            super(message);
        }
    }

    @Test
    void testNullSpansListReturnsEmpty() {
        ChatClient chatClient = mockClient(new LlmPiiDetector.Response(null));

        List<LlmPiiDetector.Span> spans = LlmPiiDetector.detect(
            GUARDRAIL, chatClient, "anything", List.of("EMAIL"));

        assertThat(spans).isEmpty();
    }

    @Test
    void testFailureWrapsAsGuardrailUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> LlmPiiDetector.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL")))
            .isInstanceOf(GuardrailUnavailableException.class)
            .hasMessageContaining(GUARDRAIL);
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

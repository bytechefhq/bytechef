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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.GuardrailOutputParseException;
import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.definition.Context;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
class LlmPiiDetectorUtilsTest {

    private static final String GUARDRAIL = "llmPii";
    private static final Context CONTEXT = mock(Context.class);

    @Test
    void testDetectReturnsSpansParsedFromLlmEntityResponse() {
        ChatClient chatClient = mockClient(new LlmPiiDetectorUtils.Response(List.of(
            new LlmPiiDetectorUtils.Span("EMAIL", "alice@example.com"),
            new LlmPiiDetectorUtils.Span("PERSON", "Alice Adams"))));

        List<LlmPiiDetectorUtils.Span> spans = LlmPiiDetectorUtils.detect(
            GUARDRAIL, chatClient, "Contact Alice Adams at alice@example.com",
            List.of("EMAIL", "PERSON", "PHONE"), CONTEXT);

        assertThat(spans).hasSize(2);
        assertThat(spans)
            .extracting(LlmPiiDetectorUtils.Span::value)
            .containsExactly("alice@example.com", "Alice Adams");
    }

    @Test
    void testDropsHallucinatedSpansNotPresentInInput() {
        ChatClient chatClient = mockClient(new LlmPiiDetectorUtils.Response(List.of(
            new LlmPiiDetectorUtils.Span("EMAIL", "alice@example.com"),
            new LlmPiiDetectorUtils.Span("EMAIL", "bob@elsewhere.org"))));

        List<LlmPiiDetectorUtils.Span> spans = LlmPiiDetectorUtils.detect(
            GUARDRAIL, chatClient, "Contact alice@example.com", List.of("EMAIL"), CONTEXT);

        assertThat(spans)
            .extracting(LlmPiiDetectorUtils.Span::value)
            .containsExactly("alice@example.com");
    }

    @Test
    void testAllHallucinatedSpansFailClosedWithGuardrailUnavailableException() {
        ChatClient chatClient = mockClient(new LlmPiiDetectorUtils.Response(List.of(
            new LlmPiiDetectorUtils.Span("EMAIL", "ghost1@nowhere.org"),
            new LlmPiiDetectorUtils.Span("EMAIL", "ghost2@nowhere.org"))));

        assertThatThrownBy(() -> LlmPiiDetectorUtils.detect(
            GUARDRAIL, chatClient, "Contact alice@example.com", List.of("EMAIL"), CONTEXT))
                .isInstanceOf(GuardrailUnavailableException.class)
                .hasMessageContaining("100% hallucination");
    }

    @Test
    void testMajorityHallucinationAboveFiftyPercentFailsClosed() {
        ChatClient chatClient = mockClient(new LlmPiiDetectorUtils.Response(List.of(
            new LlmPiiDetectorUtils.Span("EMAIL", "alice@example.com"),
            new LlmPiiDetectorUtils.Span("EMAIL", "ghost1@nowhere.org"),
            new LlmPiiDetectorUtils.Span("EMAIL", "ghost2@nowhere.org"),
            new LlmPiiDetectorUtils.Span("EMAIL", "ghost3@nowhere.org"),
            new LlmPiiDetectorUtils.Span("EMAIL", "ghost4@nowhere.org"))));

        assertThatThrownBy(() -> LlmPiiDetectorUtils.detect(
            GUARDRAIL, chatClient, "Contact alice@example.com", List.of("EMAIL"), CONTEXT))
                .isInstanceOf(GuardrailUnavailableException.class)
                .hasMessageContaining("dropped 4 of 5");
    }

    @Test
    void testEmptyInputReturnsEmpty() {
        ChatClient chatClient = mock(ChatClient.class);

        assertThat(LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "", List.of("EMAIL"), CONTEXT)).isEmpty();
        assertThat(LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, null, List.of("EMAIL"), CONTEXT)).isEmpty();
    }

    @Test
    void testNullRequestedTypesFailsClosedWithConfigurationError() {
        ChatClient chatClient = mock(ChatClient.class);

        assertThatThrownBy(
            () -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "alice@example.com", null, CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires at least one entity type");
    }

    @Test
    void testEmptyRequestedTypesFailsClosedWithConfigurationError() {
        ChatClient chatClient = mock(ChatClient.class);

        assertThatThrownBy(() -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "anything", List.of(), CONTEXT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("requires at least one entity type");
    }

    @Test
    void testNullParsedResponseFailsClosedWithGuardrailUnavailableException() {
        ChatClient chatClient = mockClient(null);

        assertThatThrownBy(
            () -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL"), CONTEXT))
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
        when(spec.messages(anyList())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenThrow(new TestJacksonException("unexpected token"));

        assertThatThrownBy(
            () -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL"), CONTEXT))
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
    void testNullSpansListFailsClosed() {
        ChatClient chatClient = mockClient(new LlmPiiDetectorUtils.Response(null));

        assertThatThrownBy(
            () -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL"), CONTEXT))
                .isInstanceOf(GuardrailUnavailableException.class)
                .hasMessageContaining("null spans field");
    }

    @Test
    void testWhitespaceOnlySpanValueIsRejectedAsHallucination() {
        // A space character is present in essentially every input, so a span value of " " would otherwise
        // pass the text.contains() check and then mask every space in the response. Guard against this by
        // rejecting blank values.
        LlmPiiDetectorUtils.Response response = new LlmPiiDetectorUtils.Response(
            List.of(new LlmPiiDetectorUtils.Span("PERSON", " ")));
        ChatClient chatClient = mockClient(response);

        assertThatThrownBy(
            () -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "alice met bob today", List.of("PERSON"),
                CONTEXT))
                    .isInstanceOf(GuardrailUnavailableException.class)
                    .hasMessageContaining("100% hallucination");
    }

    @Test
    void testFailureWrapsAsGuardrailUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.messages(anyList())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(
            () -> LlmPiiDetectorUtils.detect(GUARDRAIL, chatClient, "anything", List.of("EMAIL"), CONTEXT))
                .isInstanceOf(GuardrailUnavailableException.class)
                .hasMessageContaining(GUARDRAIL);
    }

    private static ChatClient mockClient(LlmPiiDetectorUtils.Response response) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.messages(anyList())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenReturn(response);

        return chatClient;
    }
}

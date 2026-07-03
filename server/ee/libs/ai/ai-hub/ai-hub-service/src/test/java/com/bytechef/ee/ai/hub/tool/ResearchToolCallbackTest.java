/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResearchToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsReportWhenSubagentSucceeds() throws Exception {
        String report = "## Research findings\n\nKey insight one. Key insight two.";

        ChatClient researchChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(researchChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(report);

        ResearchToolCallback callback = new ResearchToolCallback(researchChatClient);

        String result = callback.call("{\"topic\":\"climate adaptation in 2026\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("report")).isTrue();
        assertThat(node.get("report")
            .asText()).isEqualTo(report);
    }

    @Test
    void testCallReturnsErrorWhenTopicIsBlank() {
        ChatClient researchChatClient = mock(ChatClient.class);

        ResearchToolCallback callback = new ResearchToolCallback(researchChatClient);

        String result = callback.call("{\"topic\":\"  \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("topic is required");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient researchChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(researchChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        ResearchToolCallback callback = new ResearchToolCallback(researchChatClient);

        String result = callback.call("{\"topic\":\"any topic\"}");

        JsonNode node = jsonMapper.readTree(result);

        // A null subagent result must surface as a typed tool error so the parent agent does not synthesise an
        // answer from an empty report.
        assertThat(node.has("error"))
            .as("null subagent result must surface as a tool error, not an empty report")
            .isTrue();
        assertThat(node.has("report")).isFalse();
        assertThat(node.get("error")
            .asText()).containsIgnoringCase("research subagent returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient researchChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(researchChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("upstream model 503"));

        ResearchToolCallback callback = new ResearchToolCallback(researchChatClient);

        String result = callback.call("{\"topic\":\"any topic\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name for the LLM to recover")
                .contains("research failed")
                .as("payload must NOT leak the exception's getMessage() text — see ToolErrors.runtimeFailure")
                .doesNotContain("upstream model 503");
    }

    /**
     * Catch-narrowing regression guard for the ResearchToolCallback. A future refactor that narrows the
     * {@code catch (RuntimeException)} arm would let the non-WebClient types leak again.
     */
    private static Stream<Arguments> upstreamFailures() {
        return Stream.of(
            Arguments.of(WebClientResponseException.create(400, "Bad Request", null, null, null)),
            Arguments.of(WebClientResponseException.create(503, "Service Unavailable", null, null, null)),
            Arguments.of(new RuntimeException(new IOException("connection reset"))),
            Arguments.of(new RuntimeException(new TimeoutException("upstream timeout"))),
            Arguments.of(new NullPointerException("malformed response")));
    }

    @ParameterizedTest
    @MethodSource("upstreamFailures")
    void testCallSurfacesAllRuntimeExceptionTypesAsToolError(RuntimeException upstreamException) throws Exception {
        ChatClient researchChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(researchChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        ResearchToolCallback callback = new ResearchToolCallback(researchChatClient);

        String result = callback.call("{\"topic\":\"any topic\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("every upstream RuntimeException must produce a typed tool-error payload, not propagate")
            .isTrue();
        assertThat(node.get("error")
            .asText()).contains("research failed");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        ChatClient researchChatClient = mock(ChatClient.class);

        ResearchToolCallback callback = new ResearchToolCallback(researchChatClient);

        String result = callback.call("not-json");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("invalid tool input");
    }

    private static void stubToolContext(ChatClientRequestSpec requestSpec) {
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    }
}

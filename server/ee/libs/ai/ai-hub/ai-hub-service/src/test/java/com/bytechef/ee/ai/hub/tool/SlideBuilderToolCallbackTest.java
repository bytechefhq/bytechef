/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
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
class SlideBuilderToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsSummaryWhenSubagentSucceeds() {
        String summary = "Built a 7-slide deck on Q3 roadmap and saved it as roadmap.pptx.";

        ChatClient slideBuilderChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(slideBuilderChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(summary);

        SlideBuilderToolCallback callback = new SlideBuilderToolCallback(slideBuilderChatClient);

        String result = callback.call("{\"topic\":\"Q3 roadmap\"}");

        assertThat(result).isEqualTo(summary);
    }

    @Test
    void testCallForwardsOptionalFieldsIntoPrompt() {
        ChatClient slideBuilderChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(slideBuilderChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        SlideBuilderToolCallback callback = new SlideBuilderToolCallback(slideBuilderChatClient);

        callback.call(
            "{\"topic\":\"product launch\",\"outlineOrSourceFileId\":\"file-7\","
                + "\"filename\":\"launch.pptx\",\"slideCount\":8}");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        org.mockito.Mockito.verify(slideBuilderChatClient)
            .prompt(promptCaptor.capture());

        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("product launch");
        assertThat(prompt).contains("file-7");
        assertThat(prompt).contains("launch.pptx");
        assertThat(prompt).contains("8");
    }

    @Test
    void testCallReturnsErrorWhenTopicIsBlank() {
        ChatClient slideBuilderChatClient = mock(ChatClient.class);

        SlideBuilderToolCallback callback = new SlideBuilderToolCallback(slideBuilderChatClient);

        String result = callback.call("{\"topic\":\"  \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("topic is required");
    }

    @Test
    void testCallReturnsErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient slideBuilderChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(slideBuilderChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        SlideBuilderToolCallback callback = new SlideBuilderToolCallback(slideBuilderChatClient);

        String result = callback.call("{\"topic\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient slideBuilderChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(slideBuilderChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("pptx assembly failed"));

        SlideBuilderToolCallback callback = new SlideBuilderToolCallback(slideBuilderChatClient);

        String result = callback.call("{\"topic\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name for the LLM to recover")
                .contains("slide_builder failed")
                .as("payload must NOT leak the exception's getMessage() text — see ToolErrors.runtimeFailure")
                .doesNotContain("pptx assembly failed");
    }

    /**
     * Catch-narrowing regression guard for the SlideBuilderToolCallback. See DataAnalystToolCallbackTest for the shared
     * rationale.
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
        ChatClient slideBuilderChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(slideBuilderChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        SlideBuilderToolCallback callback = new SlideBuilderToolCallback(slideBuilderChatClient);

        String result = callback.call("{\"topic\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("slide_builder failed");
    }
}

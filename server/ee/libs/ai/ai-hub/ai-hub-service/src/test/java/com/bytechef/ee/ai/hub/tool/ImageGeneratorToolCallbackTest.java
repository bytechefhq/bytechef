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
import static org.mockito.Mockito.verify;
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
class ImageGeneratorToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsSummaryWhenSubagentSucceeds() {
        String summary = "Generated banner image and saved as banner.png.";

        ChatClient imageGeneratorChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(imageGeneratorChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(summary);

        ImageGeneratorToolCallback callback = new ImageGeneratorToolCallback(imageGeneratorChatClient);

        String result = callback.call("{\"prompt\":\"a sunny beach\"}");

        assertThat(result).isEqualTo(summary);
    }

    @Test
    void testCallForwardsOptionalFieldsIntoPrompt() {
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(imageGeneratorChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        ImageGeneratorToolCallback callback = new ImageGeneratorToolCallback(imageGeneratorChatClient);

        callback.call(
            "{\"prompt\":\"hero banner\",\"size\":\"1792x1024\",\"style\":\"photorealistic\","
                + "\"filename\":\"hero.png\"}");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(imageGeneratorChatClient).prompt(promptCaptor.capture());

        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("hero banner");
        assertThat(prompt).contains("1792x1024");
        assertThat(prompt).contains("photorealistic");
        assertThat(prompt).contains("hero.png");
    }

    @Test
    void testCallReturnsErrorWhenPromptIsBlank() {
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);

        ImageGeneratorToolCallback callback = new ImageGeneratorToolCallback(imageGeneratorChatClient);

        String result = callback.call("{\"prompt\":\"  \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("prompt is required");
    }

    @Test
    void testCallReturnsErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(imageGeneratorChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        ImageGeneratorToolCallback callback = new ImageGeneratorToolCallback(imageGeneratorChatClient);

        String result = callback.call("{\"prompt\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(imageGeneratorChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("OpenAI rate limit"));

        ImageGeneratorToolCallback callback = new ImageGeneratorToolCallback(imageGeneratorChatClient);

        String result = callback.call("{\"prompt\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name for the LLM to recover")
                .contains("image_generator failed")
                .as("payload must NOT leak the exception's getMessage() text — see ToolErrors.runtimeFailure")
                .doesNotContain("OpenAI rate limit");
    }

    /**
     * Catch-narrowing regression guard for the ImageGeneratorToolCallback. See DataAnalystToolCallbackTest for the
     * shared rationale.
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
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(imageGeneratorChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        ImageGeneratorToolCallback callback = new ImageGeneratorToolCallback(imageGeneratorChatClient);

        String result = callback.call("{\"prompt\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("image_generator failed");
    }
}

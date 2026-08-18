/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CustomComponentAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsResultWhenSubagentSucceeds() {
        String synthesised = "## Custom Components\n\n1. my-component — created and compiled.";

        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(synthesised);

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        String result = callback.call("{\"request\":\"list my custom components\"}");

        assertThat(result).isEqualTo(synthesised);
    }

    @Test
    void testCallReturnsErrorWhenRequestIsBlank() {
        ChatClient customComponentChatClient = mock(ChatClient.class);

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        String result = callback.call("{\"request\":\"   \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("request is required");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        ChatClient customComponentChatClient = mock(ChatClient.class);

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        String result = callback.call("not-json");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("invalid tool input");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        String result = callback.call("{\"request\":\"any request\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("null subagent result must surface as a tool error")
            .isTrue();
        assertThat(node.get("error")
            .asText()).containsIgnoringCase("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("component repository unavailable"));

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name for the LLM to recover")
                .contains("buildCustomComponent failed")
                .as("payload must NOT leak the exception's getMessage() text — see ToolErrors.runtimeFailure")
                .doesNotContain("component repository unavailable");
    }

    @Test
    void testCallForwardsParentToolContextToSubagent() {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        Map<String, Object> parentContextMap = Map.of("workspaceId", 11L, "userId", 42L);

        ToolContext parentToolContext = new ToolContext(parentContextMap);

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        callback.call("{\"request\":\"any\"}", parentToolContext);

        verify(requestSpec).toolContext(parentContextMap);
    }

    @Test
    void testCallForwardsEmptyMapWhenParentToolContextIsNull() {
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        callback.call("{\"request\":\"any\"}", null);

        verify(requestSpec).toolContext(Map.of());
    }

    @Test
    void testToolDefinitionExposesCustomComponentAgentNameAndRequestSchema() {
        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> mock(ChatClient.class), null);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("buildCustomComponent");
        assertThat(callback.getToolDefinition()
            .inputSchema()).contains("\"request\"");
    }

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
        ChatClient customComponentChatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(customComponentChatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        CustomComponentAgentToolCallback callback =
            new CustomComponentAgentToolCallback(chatModel -> customComponentChatClient, null);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("every upstream RuntimeException must produce a typed tool-error payload, not propagate")
            .isTrue();
        assertThat(node.get("error")
            .asText()).contains("buildCustomComponent failed");
    }

    private static void stubToolContext(ChatClientRequestSpec requestSpec) {
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    }
}

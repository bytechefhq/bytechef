/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

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
class ClusterElementAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsResultWhenSubagentSucceeds() {
        String synthesised = "Cluster: aiAgent\n - model: gpt-4o\n - chatMemory: in-memory";

        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(synthesised);

        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(chatClient);

        String result = callback.call("{\"request\":\"describe the cluster\"}");

        assertThat(result).isEqualTo(synthesised);
    }

    @Test
    void testCallReturnsErrorWhenRequestIsBlank() {
        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(mock(ChatClient.class));

        String result = callback.call("{\"request\":\"   \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("request is required");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(mock(ChatClient.class));

        String result = callback.call("not-json");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("invalid tool input");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(chatClient);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).containsIgnoringCase("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("component lookup unavailable"));

        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(chatClient);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name")
                .contains("cluster_element_agent failed")
                .as("payload must NOT leak the exception getMessage()")
                .doesNotContain("component lookup unavailable");
    }

    @Test
    void testCallForwardsParentToolContextToSubagent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        Map<String, Object> parentContextMap = Map.of("workspaceId", 11L, "userId", 42L);

        ToolContext parentToolContext = new ToolContext(parentContextMap);

        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(chatClient);

        callback.call("{\"request\":\"any\"}", parentToolContext);

        verify(requestSpec).toolContext(parentContextMap);
    }

    @Test
    void testCallForwardsEmptyMapWhenParentToolContextIsNull() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(chatClient);

        callback.call("{\"request\":\"any\"}", null);

        verify(requestSpec).toolContext(Map.of());
    }

    @Test
    void testToolDefinitionExposesClusterElementAgentNameAndRequestSchema() {
        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(mock(ChatClient.class));

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("cluster_element_agent");
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
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        ClusterElementAgentToolCallback callback = new ClusterElementAgentToolCallback(chatClient);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("cluster_element_agent failed");
    }

    private static void stubToolContext(ChatClientRequestSpec requestSpec) {
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    }
}

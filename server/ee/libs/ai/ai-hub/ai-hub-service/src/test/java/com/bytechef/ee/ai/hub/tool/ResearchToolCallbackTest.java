/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ai.copilot.tool.catalog.SubAgentModelRequest;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.util.List;
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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ResearchToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final ChatClient researchChatClient = mock(ChatClient.class);
    private final ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
    private final CallResponseSpec responseSpec = mock(CallResponseSpec.class);
    private final List<ToolCallback> webSearchToolCallbacks = List.of(mock(ToolCallback.class));

    @Test
    void testCallReturnsReportWhenSubagentSucceeds() {
        String report = "## Research findings\n\nKey insight one. Key insight two.";

        stubSubagent();

        when(responseSpec.content()).thenReturn(report);

        String result = newCallback().call("{\"topic\":\"climate adaptation in 2026\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("report")
            .asString()).isEqualTo(report);
        assertThat(node.has("error")).isFalse();
    }

    @Test
    void testCallHandsTheSubagentTheWebSearchToolsAndTheParentContext() {
        Map<String, Object> parentContext = Map.of(AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 3L);

        stubSubagent();

        when(responseSpec.content()).thenReturn("# Report");

        newCallback().call("{\"topic\":\"iPaaS vendors\"}", new ToolContext(parentContext));

        verify(researchChatClient).prompt("iPaaS vendors");
        verify(requestSpec).tools(webSearchToolCallbacks);
        verify(requestSpec).toolContext(parentContext);
    }

    @Test
    void testCallRunsTheSubagentOnTheResolvedChatModel() {
        ChatModel resolvedChatModel = mock(ChatModel.class);
        SubAgentChatModelResolver chatModelResolver = mock(SubAgentChatModelResolver.class);
        Map<String, Object> parentContext = Map.of(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L);

        when(chatModelResolver.resolve(new SubAgentModelRequest(2L, null, null))).thenReturn(resolvedChatModel);

        stubSubagent();

        when(responseSpec.content()).thenReturn("# Report");

        ResearchToolCallback callback = new ResearchToolCallback(
            candidateChatModel -> candidateChatModel == resolvedChatModel ? researchChatClient : null,
            chatModelResolver, webSearchToolCallbacks);

        String result = callback.call("{\"topic\":\"any topic\"}", new ToolContext(parentContext));

        assertThat(result).contains("# Report");
    }

    @Test
    void testCallPropagatesAnUnavailableChatModel() {
        SubAgentChatModelResolver chatModelResolver = mock(SubAgentChatModelResolver.class);

        when(chatModelResolver.resolve(eq(new SubAgentModelRequest(null, null, null))))
            .thenThrow(new ChatModelUnavailableException("no provider"));

        ResearchToolCallback callback = new ResearchToolCallback(
            candidateChatModel -> researchChatClient, chatModelResolver, webSearchToolCallbacks);

        assertThatThrownBy(() -> callback.call("{\"topic\":\"any topic\"}"))
            .isInstanceOf(ChatModelUnavailableException.class);

        verifyNoInteractions(researchChatClient);
    }

    @Test
    void testCallReturnsErrorWhenTopicIsBlank() {
        String result = newCallback().call("{\"topic\":\"  \"}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("topic is required");

        verifyNoInteractions(researchChatClient);
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() {
        stubSubagent();

        when(responseSpec.content()).thenReturn(null);

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"topic\":\"any topic\"}"));

        assertThat(node.has("report")).isFalse();
        assertThat(node.get("error")
            .asString()).containsIgnoringCase("research subagent returned null");
    }

    @Test
    void testCallReturnsToolErrorWithoutLeakingTheExceptionMessage() {
        stubSubagent();

        when(responseSpec.content()).thenThrow(new RuntimeException("upstream model 503"));

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"topic\":\"any topic\"}"));

        assertThat(node.get("error")
            .asString())
                .contains("research failed")
                .doesNotContain("upstream model 503");
    }

    private static Stream<Arguments> upstreamFailures() {
        return Stream.of(
            Arguments.of(new HttpClientErrorException(HttpStatus.BAD_REQUEST)),
            Arguments.of(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)),
            Arguments.of(new RuntimeException(new IOException("connection reset"))),
            Arguments.of(new RuntimeException(new TimeoutException("upstream timeout"))),
            Arguments.of(new NullPointerException("malformed response")));
    }

    @ParameterizedTest
    @MethodSource("upstreamFailures")
    void testCallSurfacesAllRuntimeExceptionTypesAsToolError(RuntimeException upstreamException) {
        stubSubagent();

        when(responseSpec.content()).thenThrow(upstreamException);

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"topic\":\"any topic\"}"));

        assertThat(node.get("error")
            .asString()).contains("research failed");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        String result = newCallback().call("not-json");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("invalid input");
    }

    private ResearchToolCallback newCallback() {
        return new ResearchToolCallback(candidateChatModel -> researchChatClient, null, webSearchToolCallbacks);
    }

    private void stubSubagent() {
        when(researchChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
    }
}

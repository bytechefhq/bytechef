/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.ToolIndex;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PinnedToolSearchToolCallingAdvisorTest {

    private final ToolIndex toolIndex = mock(ToolIndex.class);
    private final ToolCallingManager toolCallingManager = mock(ToolCallingManager.class);

    @Test
    void testStaticToolsSurviveSearchNarrowingOnCallPath() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback specialist = toolCallback("buildWorkflow");
        ToolCallback resourceTool = toolCallback("listDataTables");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(specialist, resourceTool);

        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        assertThat(toolNames(afterBeforeCall)).contains("buildWorkflow", "listDataTables");
    }

    @Test
    void testStaticToolsSurviveSearchNarrowingOnStreamPath() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback specialist = toolCallback("buildWorkflow");
        ToolCallback resourceTool = toolCallback("listDataTables");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(specialist, resourceTool);

        ChatClientRequest afterInitialize = advisor.doInitializeLoopStream(request, null);
        ChatClientRequest afterBeforeStream = advisor.doBeforeStream(afterInitialize, null);

        assertThat(toolNames(afterBeforeStream)).contains("buildWorkflow", "listDataTables");
    }

    @Test
    void testDiscoveredCatalogToolBecomesCallableAfterSearch() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback agentTool = toolCallback("askUserQuestion");

        ToolCallback catalogTool = toolCallback("searchProjects");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(List.of(catalogTool));

        ToolResponseMessage searchHit = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponse("call-1", "toolSearchTool", "[\"searchProjects\"]")))
            .build();
        Prompt prompt = new Prompt(
            List.of(new SystemMessage("base system"), new UserMessage("hi"), searchHit),
            ToolCallingChatOptions.builder()
                .toolCallbacks(agentTool)
                .build());
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .context(ChatMemory.CONVERSATION_ID, "conv-1")
            .build();

        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        assertThat(toolNames(afterBeforeCall)).contains("searchProjects");
    }

    @Test
    void testStreamInitBindsRequestEnvironmentForEmbedding() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicReference<Environment> environmentDuringIndexing = new AtomicReference<>();

        doAnswer(invocation -> {
            environmentDuringIndexing.set(EnvironmentContext.fetchCurrentEnvironment());

            return null;
        }).when(toolIndex)
            .indexTools(anyString(), any());

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequestWithEnvironment(
            Environment.DEVELOPMENT.ordinal(), toolCallback("some_tool"));

        advisor.doInitializeLoopStream(request, null);

        assertThat(environmentDuringIndexing.get()).isEqualTo(Environment.DEVELOPMENT);

        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    @Test
    void testStreamInitWithoutRequestEnvironmentLeavesBindingUntouched() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicReference<Environment> environmentDuringIndexing = new AtomicReference<>();

        doAnswer(invocation -> {
            environmentDuringIndexing.set(EnvironmentContext.fetchCurrentEnvironment());

            return null;
        }).when(toolIndex)
            .indexTools(anyString(), any());

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(toolCallback("some_tool"));

        advisor.doInitializeLoopStream(request, null);

        assertThat(environmentDuringIndexing.get()).isNull();
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    @Test
    void testCatalogSupplierIsNotResolvedUntilFirstLoopInitialization() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicInteger catalogResolutions = new AtomicInteger();

        PinnedToolSearchToolCallingAdvisor advisor = new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID,
            () -> {
                catalogResolutions.incrementAndGet();

                return Map.of("searchProjects", toolCallback("searchProjects"));
            },
            () -> {});

        assertThat(catalogResolutions).hasValue(0);

        advisor.doInitializeLoop(newRequest(toolCallback("askUserQuestion")), null);

        assertThat(catalogResolutions).hasValue(1);
    }

    @Test
    void testCatalogWarmUpRunsOnFirstLoopInitialization() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicInteger warmUps = new AtomicInteger();

        PinnedToolSearchToolCallingAdvisor advisor = new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID, Map::of,
            warmUps::incrementAndGet);

        assertThat(warmUps).hasValue(0);

        advisor.doInitializeLoop(newRequest(toolCallback("askUserQuestion")), null);

        assertThat(warmUps).hasValue(1);
    }

    @Test
    void testIndexingFailureKeepsTheTurnOnThePinnedToolsOnCallPath() {
        doThrow(new IllegalStateException("No embedding provider is activated")).when(toolIndex)
            .indexTools(anyString(), any());

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(toolCallback("buildWorkflow"), toolCallback("listDataTables"));

        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        assertThat(toolNames(afterBeforeCall)).containsExactly("buildWorkflow", "listDataTables");
    }

    @Test
    void testIndexingFailureKeepsTheTurnOnThePinnedToolsOnStreamPath() {
        doThrow(new IllegalStateException("No embedding provider is activated")).when(toolIndex)
            .indexTools(anyString(), any());

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(toolCallback("buildWorkflow"), toolCallback("listDataTables"));

        ChatClientRequest afterInitialize = advisor.doInitializeLoopStream(request, null);
        ChatClientRequest afterBeforeStream = advisor.doBeforeStream(afterInitialize, null);

        assertThat(toolNames(afterBeforeStream)).containsExactly("buildWorkflow", "listDataTables");
    }

    @Test
    void testSearchToolIsOfferedWhenIndexingSucceeds() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(toolCallback("buildWorkflow"));

        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        assertThat(toolNames(afterBeforeCall)).contains("toolSearchTool", "buildWorkflow");
    }

    private PinnedToolSearchToolCallingAdvisor newAdvisor() {
        return newAdvisor(List.of());
    }

    @Test
    void testDiscoveredCatalogToolGoesThroughTheRunsToolCallbackDecorator() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback catalogTool = toolCallback("searchProjects");
        ToolCallback decoratedCatalogTool = toolCallback("searchProjects");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(List.of(catalogTool));

        ToolResponseMessage searchHit = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponse("call-1", "toolSearchTool", "[\"searchProjects\"]")))
            .build();
        Prompt prompt = new Prompt(
            List.of(new SystemMessage("base system"), new UserMessage("hi"), searchHit),
            ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallback("askUserQuestion"))
                .build());
        UnaryOperator<ToolCallback> toolCallbackDecorator =
            toolCallback -> toolCallback == catalogTool ? decoratedCatalogTool : toolCallback;
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .context(ChatMemory.CONVERSATION_ID, "conv-1")
            .context(SpringAIAgent.TOOL_CALLBACK_DECORATOR_KEY, toolCallbackDecorator)
            .build();

        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(advisor.doInitializeLoop(request, null), null);

        ToolCallingChatOptions toolCallingChatOptions = (ToolCallingChatOptions) afterBeforeCall.prompt()
            .getOptions();

        assertThat(toolCallingChatOptions.getToolCallbacks())
            .contains(decoratedCatalogTool)
            .doesNotContain(catalogTool);
        assertThat(toolCallingChatOptions.getToolContext())
            .extractingByKey(RunCatalogToolCallingManager.RUN_CATALOG_TOOL_CALLBACKS_KEY)
            .isEqualTo(Map.of("searchProjects", decoratedCatalogTool));
    }

    @Test
    void testRunCatalogToolsStayCallableWhenToolSearchIsUnavailable() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenThrow(new IllegalStateException("no embedding"));

        ToolCallback catalogTool = toolCallback("listComponents");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(List.of(catalogTool));

        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(
            advisor.doInitializeLoop(newRequest(toolCallback("askUserQuestion")), null), null);

        ToolCallingChatOptions toolCallingChatOptions = (ToolCallingChatOptions) afterBeforeCall.prompt()
            .getOptions();

        assertThat(toolNames(afterBeforeCall)).containsExactly("askUserQuestion");
        assertThat(toolCallingChatOptions.getToolContext())
            .extractingByKey(RunCatalogToolCallingManager.RUN_CATALOG_TOOL_CALLBACKS_KEY)
            .isEqualTo(Map.of("listComponents", catalogTool));
    }

    private PinnedToolSearchToolCallingAdvisor newAdvisor(List<ToolCallback> catalogToolCallbacks) {
        Map<String, ToolCallback> catalogToolCallbacksByName = catalogToolCallbacks.stream()
            .collect(Collectors.toMap(
                toolCallback -> toolCallback.getToolDefinition()
                    .name(),
                toolCallback -> toolCallback, (first, second) -> first, LinkedHashMap::new));

        return new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID, () -> catalogToolCallbacksByName, () -> {});
    }

    private static ChatClientRequest newRequest(ToolCallback... toolCallbacks) {
        Prompt prompt = new Prompt(
            List.of(new SystemMessage("base system"), new UserMessage("hi")),
            ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build());

        return ChatClientRequest.builder()
            .prompt(prompt)
            .context(ChatMemory.CONVERSATION_ID, "conv-1")
            .build();
    }

    private static ChatClientRequest newRequestWithEnvironment(int environmentOrdinal, ToolCallback... toolCallbacks) {
        Prompt prompt = new Prompt(
            List.of(new SystemMessage("base system"), new UserMessage("hi")),
            ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build());

        return ChatClientRequest.builder()
            .prompt(prompt)
            .context(ChatMemory.CONVERSATION_ID, "conv-1")
            .context(AiHubStateKeys.ENVIRONMENT_ID, (long) environmentOrdinal)
            .build();
    }

    private static List<String> toolNames(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        ToolCallingChatOptions toolCallingChatOptions = (ToolCallingChatOptions) Objects.requireNonNull(
            prompt.getOptions(), "prompt options");

        List<ToolCallback> toolCallbacks = Objects.requireNonNull(
            toolCallingChatOptions.getToolCallbacks(), "tool callbacks");

        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    private static ToolCallback toolCallback(String name) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build());

        return toolCallback;
    }
}

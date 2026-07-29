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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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
    void testPinnedToolSurvivesSearchNarrowingOnCallPath() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback pinned = toolCallback("workflow_editor_agent");
        ToolCallback searchable = toolCallback("some_other_tool");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(Set.of("workflow_editor_agent"));

        ChatClientRequest request = newRequest(pinned, searchable);

        // Drive the loop the way ToolCallingAdvisor does: initialize once (the full static tool list is on the
        // options here), then prepare the first iteration (the base class narrows the list to {searchTool}).
        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        // The pinned specialist must reappear even though the base advisor dropped every static tool; the
        // non-pinned tool must stay dropped (it is reachable only via a searchTool hit). Pre-fix, the pinned tool
        // was also gone, so the model's direct call would fail with "No ToolCallback found".
        assertThat(toolNames(afterBeforeCall)).contains("workflow_editor_agent")
            .doesNotContain("some_other_tool");
    }

    @Test
    void testPinnedToolSurvivesSearchNarrowingOnStreamPath() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback pinned = toolCallback("workflow_editor_agent");
        ToolCallback searchable = toolCallback("some_other_tool");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(Set.of("workflow_editor_agent"));

        ChatClientRequest request = newRequest(pinned, searchable);

        ChatClientRequest afterInitialize = advisor.doInitializeLoopStream(request, null);
        ChatClientRequest afterBeforeStream = advisor.doBeforeStream(afterInitialize, null);

        assertThat(toolNames(afterBeforeStream)).contains("workflow_editor_agent")
            .doesNotContain("some_other_tool");
    }

    @Test
    void testNoPinnedToolPresentLeavesIterationUnchanged() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback searchable = toolCallback("some_other_tool");

        // None of the pinned names are registered for this run (e.g. the specialist's ChatClient bean is disabled),
        // so pinning is a no-op and the search-only narrowing stands.
        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(Set.of("workflow_editor_agent"));

        ChatClientRequest request = newRequest(searchable);

        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        assertThat(toolNames(afterBeforeCall)).doesNotContain("some_other_tool", "workflow_editor_agent");
    }

    @Test
    void testDiscoveredCatalogToolBecomesCallableAfterSearch() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        // Tier-1 tool on the agent options list (mirrors askUserQuestion et al.).
        ToolCallback agentTool = toolCallback("askUserQuestion");

        // Catalog tool: registered with the tool-search resolver only, deliberately NOT on the agent options list.
        ToolCallback catalogTool = toolCallback("searchProjects");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(Set.of(), List.of(catalogTool));

        // A prior searchTool result that surfaced "searchProjects" — still present in the message window.
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

        // A catalog tool discovered via searchTool but absent from the agent's options list must be surfaced as
        // callable. Pre-fix it stayed invisible (only options-listed tools seed the base advisor's
        // cachedToolCallbacks),
        // so the model could never call it and looped re-issuing searchTool.
        assertThat(toolNames(afterBeforeCall)).contains("searchProjects");
    }

    @Test
    void testStreamInitBindsRequestEnvironmentForEmbedding() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicReference<Environment> environmentDuringIndexing = new AtomicReference<>();

        // toolIndex.indexTools() stands in for the embedding call — capture the environment the ThreadLocal exposes at
        // the moment it runs, which is what CatalogEmbeddingModel.resolveDelegate() reads to pick the provider.
        doAnswer(invocation -> {
            environmentDuringIndexing.set(EnvironmentContext.fetchCurrentEnvironment());

            return null;
        }).when(toolIndex)
            .indexTools(anyString(), any());

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(Set.of());

        ChatClientRequest request = newRequestWithEnvironment(
            Environment.DEVELOPMENT.ordinal(), toolCallback("some_tool"));

        advisor.doInitializeLoopStream(request, null);

        // Bound from the advisor request context for the duration of the indexing (embedding) call...
        assertThat(environmentDuringIndexing.get()).isEqualTo(Environment.DEVELOPMENT);

        // ...and restored to the prior (unbound) state once the hook returns, so it never leaks to the pooled worker.
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

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(Set.of());

        // No environmentId in the request context: the advisor must not fabricate a binding (the outer agent context
        // propagation still governs), so indexing runs under the unbound ThreadLocal.
        ChatClientRequest request = newRequest(toolCallback("some_tool"));

        advisor.doInitializeLoopStream(request, null);

        assertThat(environmentDuringIndexing.get()).isNull();
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    private PinnedToolSearchToolCallingAdvisor newAdvisor(Set<String> pinnedToolNames) {
        return newAdvisor(pinnedToolNames, List.of());
    }

    private PinnedToolSearchToolCallingAdvisor newAdvisor(
        Set<String> pinnedToolNames, List<ToolCallback> catalogToolCallbacks) {

        return new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID, pinnedToolNames, catalogToolCallbacks);
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
        return ((ToolCallingChatOptions) chatClientRequest.prompt()
            .getOptions()).getToolCallbacks()
                .stream()
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

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

        ToolCallback specialist = toolCallback("workflow_editor_agent");
        ToolCallback resourceTool = toolCallback("listDataTables");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(specialist, resourceTool);

        // Drive the loop the way ToolCallingAdvisor does: initialize once (the full static tool list is on the
        // options here), then prepare the first iteration (the base class narrows the list to {searchTool}).
        ChatClientRequest afterInitialize = advisor.doInitializeLoop(request, null);
        ChatClientRequest afterBeforeCall = advisor.doBeforeCall(afterInitialize, null);

        // EVERY static tool on the agent's options list must reappear even though the base advisor dropped them all:
        // each is resolvable only while on the options list and the prompt calls each directly by name. Pre-fix only a
        // hand-listed subset was re-pinned, so a static tool the prompt named but the subset omitted (e.g.
        // listDataTables) failed with "No ToolCallback found" on the model's direct call.
        assertThat(toolNames(afterBeforeCall)).contains("workflow_editor_agent", "listDataTables");
    }

    @Test
    void testStaticToolsSurviveSearchNarrowingOnStreamPath() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ToolCallback specialist = toolCallback("workflow_editor_agent");
        ToolCallback resourceTool = toolCallback("listDataTables");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        ChatClientRequest request = newRequest(specialist, resourceTool);

        ChatClientRequest afterInitialize = advisor.doInitializeLoopStream(request, null);
        ChatClientRequest afterBeforeStream = advisor.doBeforeStream(afterInitialize, null);

        assertThat(toolNames(afterBeforeStream)).contains("workflow_editor_agent", "listDataTables");
    }

    @Test
    void testDiscoveredCatalogToolBecomesCallableAfterSearch() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        // Tier-1 tool on the agent options list (mirrors askUserQuestion et al.).
        ToolCallback agentTool = toolCallback("askUserQuestion");

        // Catalog tool: registered with the tool-search resolver only, deliberately NOT on the agent options list.
        ToolCallback catalogTool = toolCallback("searchProjects");

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor(List.of(catalogTool));

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

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

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

        PinnedToolSearchToolCallingAdvisor advisor = newAdvisor();

        // No environmentId in the request context: the advisor must not fabricate a binding (the outer agent context
        // propagation still governs), so indexing runs under the unbound ThreadLocal.
        ChatClientRequest request = newRequest(toolCallback("some_tool"));

        advisor.doInitializeLoopStream(request, null);

        assertThat(environmentDuringIndexing.get()).isNull();
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }

    @Test
    void testCatalogSupplierIsNotResolvedUntilFirstLoopInitialization() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicInteger catalogResolutions = new AtomicInteger();

        // Mirrors the memoised supplier the config hands the advisor; the count records how many times the catalog
        // (which forces the full component definition load) was materialised.
        PinnedToolSearchToolCallingAdvisor advisor = new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID,
            () -> {
                catalogResolutions.incrementAndGet();

                return Map.of("searchProjects", toolCallback("searchProjects"));
            },
            () -> {});

        // Construction must not touch the catalog — that is the whole point of the deferral, so boot stays lazy.
        assertThat(catalogResolutions).hasValue(0);

        advisor.doInitializeLoop(newRequest(toolCallback("askUserQuestion")), null);

        // The first chat turn's loop initialization resolves it exactly once.
        assertThat(catalogResolutions).hasValue(1);
    }

    @Test
    void testCatalogWarmUpRunsOnFirstLoopInitialization() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        AtomicInteger warmUps = new AtomicInteger();

        PinnedToolSearchToolCallingAdvisor advisor = new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID, Map::of,
            warmUps::incrementAndGet);

        // The pgvector index warm-up must not run at construction (that would force the catalog load at startup)...
        assertThat(warmUps).hasValue(0);

        advisor.doInitializeLoop(newRequest(toolCallback("askUserQuestion")), null);

        // ...but must run when the first chat turn initializes the loop, before the turn's first searchTool query.
        assertThat(warmUps).hasValue(1);
    }

    @Test
    void testSeedingCatalogDoesNotForceLazyToolSchemas() {
        when(toolCallingManager.resolveToolDefinitions(any())).thenReturn(List.of());

        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        ClusterElementToolCallback lazyCallback = new ClusterElementToolCallback(
            "slack_sendMessage", "Send a Slack message", "slack", 1, "sendMessage",
            clusterElementDefinitionService, connectionService);

        PinnedToolSearchToolCallingAdvisor advisor = new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, toolIndex, 5, ChatMemory.CONVERSATION_ID,
            () -> Map.of("slack_sendMessage", lazyCallback), () -> {});

        advisor.doInitializeLoop(newRequest(toolCallback("askUserQuestion")), null);

        // Seeding the catalog into the base advisor's cache must key off the map, never resolve the lazy callback's
        // component — verify the schema source (the definition service) was never touched.
        verifyNoInteractions(clusterElementDefinitionService);
    }

    private PinnedToolSearchToolCallingAdvisor newAdvisor() {
        return newAdvisor(List.of());
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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the management MCP exposure of this contributor's two surfaces: the catalog-backed intelligent-tool delegates
 * ({@code buildCustomComponent}/{@code buildCodeWorkflow}), and the flat context-store CRUD tool set (ticket 732,
 * CRUD-delegate-unwind Task 7, replacing the dissolved {@code context_store_agent} delegate).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationCopilotMcpContributorConfigurationTest {

    private static final Set<String> EXPECTED_CONTEXT_STORE_TOOL_NAMES = Set.of(
        "listContextSources", "searchContextStore", "getContextStoreRecord", "listAvailableSourceComponents",
        "describeSourceComponentEntities", "semanticSearchContextStore", "createContextStoreSource",
        "updateContextStoreSource", "deleteContextStoreSource", "refreshContextStoreSource",
        "setContextStoreSourceEnabled", "deleteContextStore");

    private final AutomationCopilotMcpContributorConfiguration configuration =
        new AutomationCopilotMcpContributorConfiguration();

    @Test
    void testContributesAllIntelligentToolsWhenChatClientsPresent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildCustomComponent"), intelligentDefinition("buildCodeWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            intelligentToolCatalog, mock(WorkspaceService.class));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactly("buildCustomComponent", "buildCodeWorkflow");
    }

    @Test
    void testNoIntelligentToolsWhenCatalogEmpty() {
        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            catalogOf(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testContextStoreFlatCrudContributesExactlyTheTwelveToolNames() {
        McpServerToolCallbackContributor contributor = configuration.contextStoreFlatCrudMcpContributor(
            presentContextStoreFactory(), mock(WorkspaceService.class));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactlyInAnyOrderElementsOf(EXPECTED_CONTEXT_STORE_TOOL_NAMES);
    }

    @Test
    void testContextStoreFlatCrudReturnsEmptyListWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.contextStoreFlatCrudMcpContributor(
            absentContextStoreFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testContextStoreFlatCrudToolsAcceptWorkspaceId() {
        McpServerToolCallbackContributor contributor = configuration.contextStoreFlatCrudMcpContributor(
            presentContextStoreFactory(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    private static IntelligentToolDefinition intelligentDefinition(String name) {
        ChatClient chatClient = mock(ChatClient.class);
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition())
            .thenReturn(ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{}")
                .build());

        return new FakeIntelligentToolDefinition(
            name, Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient),
            toolCallback);
    }

    private static IntelligentToolCatalog catalogOf(IntelligentToolDefinition... definitions) {
        IntelligentToolContributor contributor = () -> List.of(definitions);

        return new IntelligentToolCatalog(fixedObjectProvider(contributor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedObjectProvider(
        IntelligentToolContributor contributor) {

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ContextStoreToolCallbacksFactory> presentContextStoreFactory() {
        ContextStoreToolCallbacksFactory factory = new ContextStoreToolCallbacksFactory(
            mock(WorkspaceContextStoreSourceService.class), mock(ContextStoreQueryService.class),
            mock(ContextStoreSourceFacade.class), mock(ContextStoreFacade.class),
            mock(ContextStoreSemanticSearchService.class), mock(ClusterElementDefinitionService.class));

        ObjectProvider<ContextStoreToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ContextStoreToolCallbacksFactory> absentContextStoreFactory() {
        return mock(ObjectProvider.class);
    }

    private static final class FakeIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant;
        private final ToolCallback toolCallback;

        private FakeIntelligentToolDefinition(
            String name, Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant,
            ToolCallback toolCallback) {

            this.name = name;
            this.chatClientFactoriesByVariant = chatClientFactoriesByVariant;
            this.toolCallback = toolCallback;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String agentTypeKey() {
            return name;
        }

        @Override
        public Set<IntelligentToolScope> panelScopes() {
            return Set.of();
        }

        @Override
        @Nullable
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return chatClientFactoriesByVariant.get(variant);
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            return toolCallback;
        }
    }
}

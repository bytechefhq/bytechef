/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the management MCP exposure of the EE Copilot domain subagents: each present BUILD subagent chat client
 * contributes its delegate tool, and a missing bean (surface toggles off, context-store feature disabled) skips
 * silently instead of failing the contributor.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationCopilotMcpContributorConfigurationTest {

    private final AutomationCopilotMcpContributorConfiguration configuration =
        new AutomationCopilotMcpContributorConfiguration();

    @Test
    void testContributesAllAgentsWhenChatClientsPresent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildCustomComponent"), intelligentDefinition("buildCodeWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toPresentProvider(), intelligentToolCatalog, mock(WorkspaceService.class));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactly("context_store_agent", "buildCustomComponent", "buildCodeWorkflow");
    }

    @Test
    void testMissingChatClientsAreSkipped() {
        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toAbsentProvider(), catalogOf(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testPartialAvailabilityContributesOnlyPresentAgents() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildCustomComponent"), intelligentDefinition("buildCodeWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toAbsentProvider(), intelligentToolCatalog, mock(WorkspaceService.class));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactly("buildCustomComponent", "buildCodeWorkflow");
    }

    @Test
    void testContributedAgentToolsAcceptWorkspaceId() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildCustomComponent"), intelligentDefinition("buildCodeWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toPresentProvider(), intelligentToolCatalog, mock(WorkspaceService.class));

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

    private ObjectProvider<ChatClient> toPresentProvider() {
        ChatClient chatClient = mock(ChatClient.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            Consumer<ChatClient> dependencyConsumer = invocation.getArgument(0);

            dependencyConsumer.accept(chatClient);

            return null;
        }).when(chatClientProvider)
            .ifAvailable(any());

        return chatClientProvider;
    }

    private ObjectProvider<ChatClient> toAbsentProvider() {
        // A plain mock's ifAvailable is a no-op, mirroring Spring's behaviour for an absent bean.
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);

        return chatClientProvider;
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

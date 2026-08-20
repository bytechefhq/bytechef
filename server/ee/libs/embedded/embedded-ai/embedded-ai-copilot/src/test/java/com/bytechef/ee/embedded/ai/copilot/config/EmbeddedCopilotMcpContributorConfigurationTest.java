/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

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
 * Verifies the embedded contributor exposes the buildIntegrationWorkflow delegate workspace-scoped. Whether the
 * definition is contributed at all — i.e. whether its ChatClient bean is present — is
 * {@code EmbeddedIntelligentToolContributorConfigurationTest}'s concern, not this class's: this contributor is a pure
 * {@link IntelligentToolCatalog#getByNames} filter over whatever the catalog already contains.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedCopilotMcpContributorConfigurationTest {

    private final EmbeddedCopilotMcpContributorConfiguration configuration =
        new EmbeddedCopilotMcpContributorConfiguration();

    @Test
    void testContributesWorkspaceScopedEmbeddedAgent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(intelligentDefinition("buildIntegrationWorkflow"));

        McpServerToolCallbackContributor contributor =
            configuration.embeddedWorkflowEditorMcpToolCallbackContributor(
                intelligentToolCatalog, mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).singleElement()
            .satisfies(toolCallback -> {
                assertThat(toolCallback.getToolDefinition()
                    .name()).isEqualTo("buildIntegrationWorkflow");
                assertThat(toolCallback.getToolDefinition()
                    .inputSchema()).contains("workspaceId");
            });
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

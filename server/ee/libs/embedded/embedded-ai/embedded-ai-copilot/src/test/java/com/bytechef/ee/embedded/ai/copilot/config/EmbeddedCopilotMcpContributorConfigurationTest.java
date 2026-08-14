/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Verifies the embedded contributor exposes the integration_workflow_agent delegate workspace-scoped, and skips an
 * absent ChatClient bean.
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
        McpServerToolCallbackContributor contributor =
            configuration.embeddedWorkflowEditorMcpToolCallbackContributor(
                toPresentProvider(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).singleElement()
            .satisfies(toolCallback -> {
                assertThat(toolCallback.getToolDefinition()
                    .name()).isEqualTo("integration_workflow_agent");
                assertThat(toolCallback.getToolDefinition()
                    .inputSchema()).contains("workspaceId");
            });
    }

    @Test
    void testMissingChatClientIsSkipped() {
        McpServerToolCallbackContributor contributor =
            configuration.embeddedWorkflowEditorMcpToolCallbackContributor(
                toAbsentProvider(), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
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
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);

        return chatClientProvider;
    }
}

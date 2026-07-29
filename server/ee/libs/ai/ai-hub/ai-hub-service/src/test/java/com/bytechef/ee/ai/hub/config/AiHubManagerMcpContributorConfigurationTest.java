/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubManagerMcpContributorConfigurationTest {

    private final AiHubManagerMcpContributorConfiguration configuration =
        new AiHubManagerMcpContributorConfiguration();

    @Test
    void testContributesAllManagersWhenAllChatClientsPresent() {
        McpServerToolCallbackContributor contributor = configuration.aiHubManagerToolCallbackContributor(
            toPresentProvider(), toPresentProvider(), toPresentProvider(), toPresentProvider(),
            mock(WorkspaceService.class));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactly(
            "mcp_manager", "personal_agent_manager", "deployment_manager", "api_collection_manager");
    }

    @Test
    void testMissingChatClientsAreSkipped() {
        McpServerToolCallbackContributor contributor = configuration.aiHubManagerToolCallbackContributor(
            toPresentProvider(), toAbsentProvider(), toAbsentProvider(), toAbsentProvider(),
            mock(WorkspaceService.class));

        List<ToolCallback> toolCallbacks = contributor.getToolCallbacks();

        assertThat(toolCallbacks).hasSize(1);
        assertThat(toolCallbacks.getFirst()
            .getToolDefinition()
            .name()).isEqualTo("mcp_manager");
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
}

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

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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
        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toPresentProvider(), toPresentProvider(), toPresentProvider());

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactly("context_store_agent", "custom_component_agent", "code_workflow_agent");
    }

    @Test
    void testMissingChatClientsAreSkipped() {
        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toAbsentProvider(), toAbsentProvider(), toAbsentProvider());

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testPartialAvailabilityContributesOnlyPresentAgents() {
        McpServerToolCallbackContributor contributor = configuration.automationCopilotAgentToolCallbackContributor(
            toAbsentProvider(), toPresentProvider(), toPresentProvider());

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactly("custom_component_agent", "code_workflow_agent");
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

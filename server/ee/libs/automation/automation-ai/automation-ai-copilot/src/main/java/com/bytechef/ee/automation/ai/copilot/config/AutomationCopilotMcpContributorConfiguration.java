/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.ai.copilot.tool.CodeWorkflowAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.ContextStoreAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.CustomComponentAgentToolCallback;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the EE Copilot domain subagents ({@code context_store_agent}, {@code custom_component_agent},
 * {@code code_workflow_agent}) to the management MCP server through the {@link McpServerToolCallbackContributor} SPI —
 * the EE counterpart of the CE {@code ToolCallbackContributorConfiguration}, which contributes only CE-domain
 * subagents. Each domain exposes its BUILD subagent {@link ChatClient} (defined in
 * {@link AutomationCopilotConfiguration} / {@link ContextStoreAgentConfiguration}), whose tool set includes the
 * read-only tools, so the single delegate tool serves both ask-style and build-style requests. An absent chat client
 * bean (surface toggles off, context-store feature disabled) skips silently. Each delegate is wrapped in
 * {@link WorkspaceScopedSubAgentToolCallback} because the management MCP surface has no AI Hub chat state to supply
 * workspace scope.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AutomationCopilotMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor automationCopilotAgentToolCallbackContributor(
        @Qualifier("contextStoreBuildSubAgentChatClient") ObjectProvider<ChatClient> contextStoreProvider,
        @Qualifier("customComponentBuildSubAgentChatClient") ObjectProvider<ChatClient> customComponentProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClient") ObjectProvider<ChatClient> codeWorkflowProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            contextStoreProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ContextStoreAgentToolCallback(chatClient), workspaceService)));
            customComponentProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new CustomComponentAgentToolCallback(chatClient), workspaceService)));
            codeWorkflowProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new CodeWorkflowAgentToolCallback(chatClient), workspaceService)));

            return toolCallbacks;
        };
    }
}

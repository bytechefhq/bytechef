/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.ai.hub.tool.ManagerSubAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.WorkspaceScopedManagerToolCallback;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the AI-hub-owned manager subagents ({@code mcp_manager}, {@code personal_agent_manager},
 * {@code deployment_manager}, {@code api_collection_manager}) to the management MCP server through the
 * {@link McpServerToolCallbackContributor} SPI — the same extension point the Copilot specialists use — so external MCP
 * clients can drive MCP-server setup (including fromAi tool mapping), personal agents, deployments, and API
 * collections.
 *
 * <p>
 * Each delegate is wrapped in {@link WorkspaceScopedManagerToolCallback}: the management MCP surface has no AI Hub chat
 * state, so workspace scoping is made explicit at the tool boundary instead. Missing ChatClient beans (feature module
 * absent) skip silently, mirroring how the BUILD agent registers the same delegates. The AG-UI-specific
 * {@code ProgressReportingToolCallback} wrapper is intentionally NOT applied on this surface.
 * </p>
 *
 * <p>
 * Spec: {@code docs/superpowers/specs/2026-07-18-management-mcp-manager-subagents-design.md}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubManagerMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor aiHubManagerToolCallbackContributor(
        @Qualifier("mcpManagerChatClient") ObjectProvider<ChatClient> mcpManagerChatClientProvider,
        @Qualifier("personalAgentManagerChatClient") //
        ObjectProvider<ChatClient> personalAgentManagerChatClientProvider,
        @Qualifier("deploymentManagerChatClient") ObjectProvider<ChatClient> deploymentManagerChatClientProvider,
        @Qualifier("apiCollectionManagerChatClient") //
        ObjectProvider<ChatClient> apiCollectionManagerChatClientProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            mcpManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    toWorkspaceScoped(
                        McpManagerConfiguration.createMcpManagerToolCallback(chatClient), workspaceService)));

            personalAgentManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    toWorkspaceScoped(
                        PersonalAgentManagerConfiguration.createPersonalAgentManagerToolCallback(chatClient),
                        workspaceService)));

            deploymentManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    toWorkspaceScoped(
                        DeploymentManagerConfiguration.createDeploymentManagerToolCallback(chatClient),
                        workspaceService)));

            apiCollectionManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    toWorkspaceScoped(
                        ApiCollectionManagerConfiguration.createApiCollectionManagerToolCallback(chatClient),
                        workspaceService)));

            return toolCallbacks;
        };
    }

    private static WorkspaceScopedManagerToolCallback toWorkspaceScoped(
        ManagerSubAgentToolCallback delegate, WorkspaceService workspaceService) {

        return new WorkspaceScopedManagerToolCallback(delegate, workspaceService);
    }
}

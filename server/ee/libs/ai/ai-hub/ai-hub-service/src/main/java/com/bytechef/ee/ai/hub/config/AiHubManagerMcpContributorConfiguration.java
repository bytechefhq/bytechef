/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
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
 * Contributes the AI-hub-owned {@code personal_agent_manager} subagent to the management MCP server through the
 * {@link McpServerToolCallbackContributor} SPI. The other management managers (mcp_manager, deployment_manager,
 * api_collection_manager) are automation-owned and contributed from {@code automation-ai-tool}
 * ({@code ManagerMcpContributorConfiguration} CE, {@code ApiCollectionManagerMcpContributorConfiguration} EE); only the
 * personal-agent manager remains AI-hub-specific.
 *
 * <p>
 * The delegate is wrapped in {@link WorkspaceScopedSubAgentToolCallback}: the management MCP surface has no AI Hub chat
 * state, so workspace scoping is made explicit at the tool boundary. A missing ChatClient bean skips silently.
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
    McpServerToolCallbackContributor aiHubPersonalAgentManagerToolCallbackContributor(
        @Qualifier("personalAgentManagerChatClient") //
        ObjectProvider<ChatClient> personalAgentManagerChatClientProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            personalAgentManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        PersonalAgentManagerConfiguration.createPersonalAgentManagerToolCallback(chatClient),
                        workspaceService)));

            return toolCallbacks;
        };
    }
}

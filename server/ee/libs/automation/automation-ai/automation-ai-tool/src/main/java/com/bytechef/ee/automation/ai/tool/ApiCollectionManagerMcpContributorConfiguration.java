/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedManagerToolCallback;
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
 * Contributes the automation-owned {@code api_collection_manager} subagent to the management MCP server through the
 * {@link McpServerToolCallbackContributor} SPI. It is EE because the API-platform facade it manages is EE; the CE
 * managers (mcp_manager, deployment_manager) are contributed from {@code ManagerMcpContributorConfiguration}.
 *
 * <p>
 * The delegate is wrapped in {@link WorkspaceScopedManagerToolCallback}: the management MCP surface has no AI Hub chat
 * state, so workspace scoping is made explicit at the tool boundary. A missing ChatClient bean skips silently. The
 * {@code bytechef.ai.hub.enabled} gate is retained so the management-MCP exposure is unchanged from when this manager
 * lived in ai-hub-service.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ApiCollectionManagerMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor apiCollectionManagerToolCallbackContributor(
        @Qualifier("apiCollectionManagerChatClient") //
        ObjectProvider<ChatClient> apiCollectionManagerChatClientProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            apiCollectionManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedManagerToolCallback(
                        ApiCollectionManagerConfiguration.createApiCollectionManagerToolCallback(chatClient),
                        workspaceService)));

            return toolCallbacks;
        };
    }
}

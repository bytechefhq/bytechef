/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
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
 * Contributes the automation-owned management manager subagents {@code mcp_manager} and {@code deployment_manager} to
 * the management MCP server through the {@link McpServerToolCallbackContributor} SPI. These manage automation resources
 * (MCP servers, project deployments) rather than AI-hub chat state, so they live in {@code automation-ai-tool} rather
 * than {@code ai-hub-service}; the AI-hub {@code personal_agent_manager} keeps its own contributor.
 *
 * <p>
 * Each delegate is wrapped in {@link WorkspaceScopedManagerToolCallback}: the management MCP surface has no AI Hub chat
 * state, so workspace scoping is made explicit at the tool boundary. A missing ChatClient bean skips silently. The
 * {@code bytechef.ai.hub.enabled} gate is retained so the management-MCP exposure of these managers is unchanged from
 * when they lived in ai-hub-service.
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ManagerMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor managerToolCallbackContributor(
        @Qualifier("mcpManagerChatClient") ObjectProvider<ChatClient> mcpManagerChatClientProvider,
        @Qualifier("deploymentManagerChatClient") ObjectProvider<ChatClient> deploymentManagerChatClientProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            mcpManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedManagerToolCallback(
                        McpManagerConfiguration.createMcpManagerToolCallback(chatClient), workspaceService)));

            deploymentManagerChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedManagerToolCallback(
                        DeploymentManagerConfiguration.createDeploymentManagerToolCallback(chatClient),
                        workspaceService)));

            return toolCallbacks;
        };
    }
}

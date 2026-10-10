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

package com.bytechef.automation.ai.mcp.tool.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.mcp.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class McpServerMcpContributorConfiguration {

    private static final String MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME = "updateMcpProjectWorkflowParameters";

    private static final Set<String> WORKSPACE_SCOPED_MCP_TOOL_NAMES = Set.of(
        "listMcpServers", "createMcpServer", "listMcpProjectWorkflows");

    @Bean
    McpServerToolCallbackContributor mcpServerCrudMcpContributor(
        ObjectProvider<McpServerToolCallbacksFactory> mcpServerToolCallbacksFactoryProvider,
        WorkspaceFacade workspaceFacade) {

        return () -> {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = mcpServerToolCallbacksFactoryProvider
                .getIfAvailable();

            if (mcpServerToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : mcpServerToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                if (MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME.equals(name)) {
                    continue;
                }

                toolCallbacks.add(
                    WORKSPACE_SCOPED_MCP_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceFacade)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }
}

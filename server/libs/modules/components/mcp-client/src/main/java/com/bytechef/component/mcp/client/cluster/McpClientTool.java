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

package com.bytechef.component.mcp.client.cluster;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.mcp.client.action.McpClientCallToolAction.ACTION_DEFINITION;
import static com.bytechef.component.mcp.client.util.McpClientUtils.createMcpSyncClient;
import static com.bytechef.component.mcp.client.util.McpClientUtils.createToolFilter;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class McpClientTool {

    private McpClientTool() {
    }

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("mcpTools")
            .title("MCP Server Tools")
            .description("Discovers and exposes tools from an MCP server to the AI Agent.")
            .type(TOOLS)
            .properties(
                ACTION_DEFINITION.getProperties()
                    .orElse(null))
            .object(() -> McpClientTool::apply);

    protected static ToolCallbackProvider apply(Parameters inputParameters, Parameters connectionParameters)
        throws Exception {

        McpSyncClient mcpSyncClient = createMcpSyncClient(inputParameters, connectionParameters);

        mcpSyncClient.initialize();

        McpToolFilter toolFilter = createToolFilter(inputParameters);

        return SyncMcpToolCallbackProvider.builder()
            .mcpClients(mcpSyncClient)
            .toolFilter(toolFilter)
            .build();
    }
}

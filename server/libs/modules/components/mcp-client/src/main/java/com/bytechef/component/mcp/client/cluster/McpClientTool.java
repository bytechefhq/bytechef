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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.ALL;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_FILTER_TYPE;
import static com.bytechef.component.mcp.client.util.McpClientUtils.createMcpSyncClient;
import static com.bytechef.component.mcp.client.util.McpClientUtils.createToolFilter;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mcp.client.util.McpClientUtils;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class McpClientTool {

    private final ConnectionDefinitionService connectionDefinitionService;

    public McpClientTool(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    public ClusterElementDefinition<ToolCallbackProviderFunction> getClusterElementDefinition() {
        return ComponentDsl.<ToolCallbackProviderFunction>clusterElement("mcpTools")
            .title("MCP Server Tools")
            .description("Discovers and exposes tools from an MCP server to the AI Agent.")
            .type(TOOLS)
            .properties(
                string(TOOL_FILTER_TYPE)
                    .label("Tools")
                    .description("Choose which tools you want to expose to the AI Agent.")
                    .options(
                        option("All", ALL),
                        option("Include", INCLUDE),
                        option("Exclude", EXCLUDE))
                    .defaultValue(ALL)
                    .required(true),
                array(TOOLS_TO_INCLUDE)
                    .label("Tools to Include")
                    .description("Select the tools you want to expose to the AI Agent.")
                    .options(
                        (ClusterElementDefinition.OptionsFunction<String>) (
                            inputParameters, connectionParameters,
                            lookupDependsOnPaths, searchText, context) -> McpClientUtils.getToolOptions(
                                connectionParameters, connectionDefinitionService, context))
                    .optionsLookupDependsOn(TOOL_FILTER_TYPE)
                    .items(string())
                    .displayCondition("%s == '%s'".formatted(TOOL_FILTER_TYPE, INCLUDE)),
                array(TOOLS_TO_EXCLUDE)
                    .label("Tools to Exclude")
                    .description(
                        "Select the tools you want to exclude. The AI Agent will have access to all other tools.")
                    .options(
                        (ClusterElementDefinition.OptionsFunction<String>) (
                            inputParameters, connectionParameters,
                            lookupDependsOnPaths, searchText, context) -> McpClientUtils.getToolOptions(
                                connectionParameters, connectionDefinitionService, context))
                    .items(string())
                    .optionsLookupDependsOn(TOOL_FILTER_TYPE)
                    .displayCondition("%s == '%s'".formatted(TOOL_FILTER_TYPE, EXCLUDE)))
            .object(() -> this::apply);
    }

    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        McpSyncClient mcpSyncClient = createMcpSyncClient(
            connectionParameters, connectionDefinitionService, context);

        mcpSyncClient.initialize();

        McpToolFilter toolFilter = createToolFilter(inputParameters);

        return SyncMcpToolCallbackProvider.builder()
            .mcpClients(mcpSyncClient)
            .toolFilter(toolFilter)
            .build();
    }
}

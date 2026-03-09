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

package com.bytechef.component.mcp.client.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_ARGUMENTS;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_NAME;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TRANSPORT_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.URL;
import static com.bytechef.component.mcp.client.util.McpClientUtils.createMcpSyncClient;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mcp.client.util.McpClientUtils;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class McpClientCallToolAction {

    private McpClientCallToolAction() {
    }

    public static ModifiableActionDefinition of(ConnectionDefinitionService connectionDefinitionService) {
        return action("callTool")
            .title("Call Tool")
            .description("Connects to an MCP server and calls a specific tool with the provided arguments.")
            .properties(
                string(TOOL_NAME)
                    .label("Tool Name")
                    .description("The name of the tool to call on the MCP server.")
                    .options(
                        (ActionDefinition.OptionsFunction<String>) (
                            inputParameters, connectionParameters,
                            lookupDependsOnPaths, searchText, context) -> McpClientUtils.getToolOptions(
                                connectionParameters, connectionDefinitionService, context))
                    .optionsLookupDependsOn(URL, TRANSPORT_TYPE)
                    .required(true),
                dynamicProperties(TOOL_ARGUMENTS)
                    .propertiesLookupDependsOn(TOOL_NAME)
                    .properties(
                        (ActionDefinition.PropertiesFunction) (
                            inputParameters, connectionParameters,
                            lookupDependsOnPaths, context) -> McpClientUtils.getToolProperties(
                                inputParameters, connectionParameters, connectionDefinitionService, context)))
            .perform(
                (ActionDefinition.PerformFunction) (inputParameters, connectionParameters, actionContext) -> perform(
                    inputParameters, connectionParameters, connectionDefinitionService, actionContext));
    }

    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters,
        ConnectionDefinitionService connectionDefinitionService, ActionContext actionContext) {

        String toolName = inputParameters.getRequiredString(TOOL_NAME);
        Map<String, Object> toolArguments = inputParameters.getMap(TOOL_ARGUMENTS, Object.class, Map.of());

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(
            connectionParameters, connectionDefinitionService, actionContext)) {

            mcpSyncClient.initialize();

            return mcpSyncClient.callTool(new McpSchema.CallToolRequest(toolName, toolArguments));
        }
    }
}

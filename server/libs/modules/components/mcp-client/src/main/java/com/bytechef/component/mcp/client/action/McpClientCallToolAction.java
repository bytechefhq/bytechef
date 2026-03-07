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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.ALL;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_STREAMABLE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_FILTER_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TRANSPORT_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.URL;
import static com.bytechef.component.mcp.client.util.McpClientUtils.createMcpSyncClient;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mcp.client.util.McpClientUtils;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class McpClientCallToolAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("callTool")
        .title("Call Tool")
        .description("Connects to an MCP server and exposes its tools to the AI Agent.")
        .properties(
            string(URL)
                .label("Server URL")
                .description("The URL of the MCP server to connect to.")
                .required(true),
            string(TRANSPORT_TYPE)
                .label("Transport Type")
                .description("The transport protocol to use for connecting to the MCP server.")
                .options(
                    option("HTTP Streamable", HTTP_STREAMABLE),
                    option("SSE", "sse"))
                .defaultValue(HTTP_STREAMABLE)
                .required(true),
            string(TOOL_FILTER_TYPE)
                .label("Tools to Include")
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
                .displayCondition("%s == '%s'".formatted(TOOL_FILTER_TYPE, INCLUDE))
                .items(
                    string()
                        .options((OptionsFunction<String>) McpClientUtils::getToolOptions)
                        .optionsLookupDependsOn(URL, TRANSPORT_TYPE)),
            array(TOOLS_TO_EXCLUDE)
                .label("Tools to Exclude")
                .description(
                    "Select the tools you want to exclude. The AI Agent will have access to all other tools.")
                .displayCondition("%s == '%s'".formatted(TOOL_FILTER_TYPE, EXCLUDE))
                .items(
                    string()
                        .options((OptionsFunction<String>) McpClientUtils::getToolOptions)
                        .optionsLookupDependsOn(URL, TRANSPORT_TYPE)))
        .perform(McpClientCallToolAction::perform);

    private McpClientCallToolAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(inputParameters, connectionParameters)) {
            mcpSyncClient.initialize();

            List<McpSchema.Tool> tools = mcpSyncClient.listTools()
                .tools();

            return Map.of(
                "tools", tools.stream()
                    .map(tool -> Map.of("name", tool.name(), "description", tool.description()))
                    .toList(),
                "count", tools.size());
        }
    }
}

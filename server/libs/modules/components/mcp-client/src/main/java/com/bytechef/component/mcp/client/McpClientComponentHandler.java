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

package com.bytechef.component.mcp.client;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.MCP_CLIENT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.mcp.client.action.McpClientCallToolAction;
import com.bytechef.component.mcp.client.cluster.McpClientTool;
import com.bytechef.component.mcp.client.connection.McpClientConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class McpClientComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MCP_CLIENT)
        .title("MCP Client")
        .description("Connects to external MCP servers to discover and call their tools.")
        .icon("path:assets/mcp-client.svg")
        .categories(ComponentCategory.HELPERS, ComponentCategory.DEVELOPER_TOOLS)
        .connection(McpClientConnection.CONNECTION_DEFINITION)
        .actions(McpClientCallToolAction.ACTION_DEFINITION)
        .clusterElements(McpClientTool.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

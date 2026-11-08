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

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_SSE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_STREAMABLE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.SERVER_NAME;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TRANSPORT_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.URL;
import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.MCP_SERVERS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mcp.client.util.McpClientUtils;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.McpServer;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class McpClientClaudeCodeMcpServer {

    private final ConnectionDefinitionService connectionDefinitionService;

    public McpClientClaudeCodeMcpServer(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    public ClusterElementDefinition<ClaudeCodeMcpServerFunction> getClusterElementDefinition() {
        return ComponentDsl.<ClaudeCodeMcpServerFunction>clusterElement("mcpServer")
            .title("MCP Server")
            .description(
                "Gives Claude Code the tools of an MCP server. The connection's credentials authenticate every request.")
            .type(MCP_SERVERS)
            .properties(
                string(SERVER_NAME)
                    .label("Server Name")
                    .description(
                        "The name Claude Code gives the server, used as the prefix of its tool names. Defaults to " +
                            "the host of the server URL.")
                    .required(false))
            .object(() -> this::apply);
    }

    private McpServer apply(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String url = connectionParameters.getRequiredString(URL);
        String transportType = connectionParameters.getString(TRANSPORT_TYPE, HTTP_STREAMABLE);

        Map<String, List<String>> headers = McpClientUtils.getHeaders(
            connectionParameters, connectionDefinitionService, context);

        return new McpServer(
            getServerName(inputParameters.getString(SERVER_NAME), url),
            HTTP_SSE.equals(transportType) ? McpServer.SSE : McpServer.HTTP, url, toSingleValueHeaders(headers));
    }

    static String getServerName(String serverName, String url) {
        String name = serverName;

        if (name == null || name.isBlank()) {
            URI uri = URI.create(url);

            name = uri.getHost();
        }

        if (name == null || name.isBlank()) {
            return "mcp";
        }

        return name.strip()
            .replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static Map<String, String> toSingleValueHeaders(Map<String, List<String>> headers) {
        Map<String, String> singleValueHeaders = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            singleValueHeaders.put(entry.getKey(), String.join(", ", entry.getValue()));
        }

        return singleValueHeaders;
    }
}

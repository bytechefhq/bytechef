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

package com.bytechef.platform.component.definition.ai.claudecode;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * Describes an MCP server that a Claude Code run may use.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ClaudeCodeMcpServerFunction {

    ClusterElementType MCP_SERVERS = new ClusterElementType("MCP_SERVERS", "mcpServers", "MCP Servers", true, false);

    /**
     * @param inputParameters      The input parameters of the cluster element.
     * @param connectionParameters The connection parameters of the cluster element.
     * @param context              The context of the calling action.
     * @return The MCP server the Claude Code run connects to.
     * @throws Exception If the MCP server cannot be described.
     */
    McpServer apply(Parameters inputParameters, Parameters connectionParameters, Context context) throws Exception;

    /**
     * @param name          The name Claude Code gives the server's tools.
     * @param transportType The transport type, {@link #HTTP} or {@link #SSE}.
     * @param url           The URL of the MCP server.
     * @param headers       The HTTP headers sent with every request, for example the authorization header.
     */
    record McpServer(String name, String transportType, String url, Map<String, String> headers) {

        public static final String HTTP = "http";
        public static final String SSE = "sse";

        public McpServer {
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }
}

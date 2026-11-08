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

package com.bytechef.component.claude.code.util;

import com.bytechef.component.definition.Context;
import io.github.markpollack.agents.claude.ClaudeAgentModel;
import io.github.markpollack.agents.claude.ClaudeAgentOptions;
import io.github.markpollack.agents.model.AgentApi;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public final class ClaudeCodeUtil {

    private ClaudeCodeUtil() {
    }

    public static AgentApi createAgentModel() {
        return ClaudeAgentModel.builder()
            .defaultOptions(ClaudeAgentOptions.builder()
                .yolo(true)
                .build())
            .build();
    }

    public static String serializeMcpServer(
        String label, String url, Map<String, String> headers, Context context) {

        McpServer mcpServer = new McpServer(label, url, headers);

        return context.json(json -> json.write(mcpServer));
    }

    public static Map<String, McpServerConfig> deserializeMcpServers(
        List<String> serializedMcpServers, Context context) {

        Map<String, McpServerConfig> mcpServerConfigs = new LinkedHashMap<>();

        for (String serializedMcpServer : serializedMcpServers) {
            McpServer mcpServer = context.json(json -> json.read(serializedMcpServer, McpServer.class));

            mcpServerConfigs.put(
                mcpServer.label(), new McpServerConfig.McpHttpServerConfig(mcpServer.url(), mcpServer.headers()));
        }

        return mcpServerConfigs;
    }

    public record McpServer(String label, String url, Map<String, String> headers) {

        public McpServer {
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }
}

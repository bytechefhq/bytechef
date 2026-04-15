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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.markpollack.agents.claude.ClaudeAgentModel;
import io.github.markpollack.agents.claude.ClaudeAgentOptions;
import io.github.markpollack.agents.model.AgentApi;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public final class ClaudeCodeUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ClaudeCodeUtil() {
    }

    public static AgentApi createAgentModel() {
        return ClaudeAgentModel.builder()
            .defaultOptions(ClaudeAgentOptions.builder()
                .yolo(true)
                .build())
            .build();
    }

    public static String serializeMcpServer(String label, McpServerConfig.McpHttpServerConfig serverConfig) {
        Map<String, Object> envelope = Map.of(
            "label", label,
            "config", serverConfig);

        try {
            return OBJECT_MAPPER.writeValueAsString(envelope);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize MCP server descriptor", e);
        }
    }

    public static Map<String, McpServerConfig> deserializeMcpServers(List<String> serializedServers) {
        Map<String, McpServerConfig> servers = new LinkedHashMap<>();

        for (String serialized : serializedServers) {
            try {
                Map<String, Object> envelope = OBJECT_MAPPER.readValue(
                    serialized, new TypeReference<Map<String, Object>>() {});

                String label = (String) envelope.get("label");
                McpServerConfig config = OBJECT_MAPPER.convertValue(
                    envelope.get("config"), McpServerConfig.class);

                servers.put(label, config);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to deserialize MCP server descriptor", e);
            }
        }

        return servers;
    }
}

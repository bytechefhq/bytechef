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

package com.bytechef.platform.mcp.web.graphql;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.web.McpServerTypeDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

/**
 * The contributed {@link McpServerTypeDefinition}s by type. A type without a definition is neither administered through
 * the shared MCP operations nor given a URL.
 *
 * @author Ivica Cardic
 */
final class McpServerTypeDefinitions {

    private final Map<PlatformType, McpServerTypeDefinition> mcpServerTypeDefinitions;

    McpServerTypeDefinitions(List<McpServerTypeDefinition> mcpServerTypeDefinitions) {
        this.mcpServerTypeDefinitions = mcpServerTypeDefinitions.stream()
            .collect(Collectors.toMap(McpServerTypeDefinition::getType, Function.identity()));
    }

    void checkSharedAdministration(PlatformType type) {
        McpServerTypeDefinition mcpServerTypeDefinition = mcpServerTypeDefinitions.get(type);

        if (mcpServerTypeDefinition == null || !mcpServerTypeDefinition.isSharedAdministration()) {
            throw new AccessDeniedException(
                "MCP servers of type " + type + " can only be administered through their own MCP server operations");
        }
    }

    Set<Long> getSharedAdministrationMcpServerIds(McpServerService mcpServerService) {
        return mcpServerTypeDefinitions.values()
            .stream()
            .filter(McpServerTypeDefinition::isSharedAdministration)
            .flatMap(mcpServerTypeDefinition -> mcpServerService.getMcpServers(mcpServerTypeDefinition.getType())
                .stream())
            .map(McpServer::getId)
            .collect(Collectors.toSet());
    }

    String resolvePath(PlatformType type, String secretKey) {
        McpServerTypeDefinition mcpServerTypeDefinition = mcpServerTypeDefinitions.get(type);

        if (mcpServerTypeDefinition == null) {
            throw new IllegalStateException("No MCP server type definition for type " + type);
        }

        return mcpServerTypeDefinition.resolvePath(secretKey);
    }
}

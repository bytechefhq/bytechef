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

package com.bytechef.automation.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the environment of a {@code 'McpComponent'} through the MCP server it belongs to.
 *
 * @author Ivica Cardic
 */
@Component
public class McpComponentEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public McpComponentEnvironmentResolver(
        McpComponentService mcpComponentService, McpServerService mcpServerService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpComponent";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpComponentService.fetchMcpComponentMcpServerId(number.longValue())
            .flatMap(mcpServerService::fetchMcpServer)
            .map(McpServer::getEnvironment);
    }
}

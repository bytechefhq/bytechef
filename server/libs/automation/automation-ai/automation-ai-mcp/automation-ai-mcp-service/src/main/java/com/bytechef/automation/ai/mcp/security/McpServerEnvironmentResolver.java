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
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads the server through {@link McpServerService#fetchMcpServer(long)}, never {@code getMcpServer(long)}: that by-id
 * read is itself guarded by {@code hasPermission(#mcpServerId, 'McpServer', ...)}, which calls this resolver again.
 *
 * @author Ivica Cardic
 */
@Component
public class McpServerEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public McpServerEnvironmentResolver(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpServer";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpServerService.fetchMcpServer(number.longValue())
            .map(McpServer::getEnvironment);
    }
}

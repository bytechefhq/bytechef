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

        try {
            McpServer mcpServer = mcpServerService.getMcpServer(number.longValue());

            return Optional.of(mcpServer.getEnvironment());
        } catch (RuntimeException runtimeException) {
            return Optional.empty();
        }
    }
}

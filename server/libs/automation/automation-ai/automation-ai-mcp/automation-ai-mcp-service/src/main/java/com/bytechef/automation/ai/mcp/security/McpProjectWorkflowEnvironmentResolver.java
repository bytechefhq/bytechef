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

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the environment of a {@code 'McpProjectWorkflow'} through the MCP server it belongs to.
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectWorkflowEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpProjectRepository mcpProjectRepository;
    private final McpProjectWorkflowRepository mcpProjectWorkflowRepository;
    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowEnvironmentResolver(
        McpProjectRepository mcpProjectRepository, McpProjectWorkflowRepository mcpProjectWorkflowRepository,
        McpServerRepository mcpServerRepository) {

        this.mcpProjectRepository = mcpProjectRepository;
        this.mcpProjectWorkflowRepository = mcpProjectWorkflowRepository;
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public String resourceType() {
        return "McpProjectWorkflow";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpProjectWorkflowRepository.findById(number.longValue())
            .map(McpProjectWorkflow::getMcpProjectId)
            .flatMap(mcpProjectRepository::findById)
            .map(McpProject::getMcpServerId)
            .flatMap(mcpServerRepository::findById)
            .map(McpServer::getEnvironment);
    }
}

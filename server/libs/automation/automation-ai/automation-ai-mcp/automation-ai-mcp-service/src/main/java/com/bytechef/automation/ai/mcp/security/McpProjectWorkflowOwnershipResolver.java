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
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an MCP-project-workflow id to its owning workspace by traversing workflow &rarr; MCP project &rarr; MCP server
 * &rarr; {@code workspace_mcp_server} relation. Uses repositories directly (not the {@code @PreAuthorize}-guarded
 * services) to avoid recursion. Fails closed when any hop cannot be resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectWorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final McpProjectRepository mcpProjectRepository;
    private final McpProjectWorkflowRepository mcpProjectWorkflowRepository;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowOwnershipResolver(
        McpProjectRepository mcpProjectRepository, McpProjectWorkflowRepository mcpProjectWorkflowRepository,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpProjectRepository = mcpProjectRepository;
        this.mcpProjectWorkflowRepository = mcpProjectWorkflowRepository;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpProjectWorkflow";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpProjectWorkflowRepository.findById(id)
            .map(McpProjectWorkflow::getMcpProjectId)
            .flatMap(mcpProjectRepository::findById)
            .map(McpProject::getMcpServerId)
            .flatMap(workspaceMcpServerService::fetchWorkspaceIdByMcpServerId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}

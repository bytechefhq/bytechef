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
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an MCP-project id to its owning workspace by traversing project &rarr; MCP server &rarr;
 * {@code workspace_mcp_server} relation. Collaborative, workspace-scoped: EE authorizes by workspace role, CE is
 * permissive. Fails closed when the project (or its server's workspace) cannot be resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectOwnershipResolver implements ResourceOwnershipResolver {

    // Uses the repository (not McpProjectService) directly: McpProjectService.fetchMcpProject is itself
    // @PreAuthorize-guarded by this resolver, so going through the service would recurse.
    private final McpProjectRepository mcpProjectRepository;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpProjectOwnershipResolver(
        McpProjectRepository mcpProjectRepository, WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpProjectRepository = mcpProjectRepository;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpProject";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpProjectRepository.findById(id)
            .map(McpProject::getMcpServerId)
            .flatMap(workspaceMcpServerService::fetchWorkspaceIdByMcpServerId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}

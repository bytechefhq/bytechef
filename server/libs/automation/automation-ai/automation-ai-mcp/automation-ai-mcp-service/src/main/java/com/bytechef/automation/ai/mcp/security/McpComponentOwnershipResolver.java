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

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an MCP-component id to its owning workspace by traversing component &rarr; MCP server &rarr;
 * {@code workspace_mcp_server} relation. Uses the platform repository directly (not the {@code @PreAuthorize}-guarded
 * {@code McpComponentService}) to avoid recursion. Fails closed when the component or its server's workspace cannot be
 * resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class McpComponentOwnershipResolver implements ResourceOwnershipResolver {

    private final McpComponentRepository mcpComponentRepository;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpComponentOwnershipResolver(
        McpComponentRepository mcpComponentRepository, WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpComponent";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpComponentRepository.findById(id)
            .map(McpComponent::getMcpServerId)
            .flatMap(workspaceMcpServerService::fetchWorkspaceIdByMcpServerId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps an MCP-server id to its owning workspace via the {@code workspace_mcp_server} relation. MCP servers are
 * collaborative, workspace-scoped resources: EE authorizes by the caller's workspace role; CE is permissive (shared
 * within the single workspace). Fails closed when the server is not mapped to any workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class McpServerOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpServerOwnershipResolver(WorkspaceMcpServerService workspaceMcpServerService) {
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpServer";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<Long> workspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(id);

        return workspaceId.map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}

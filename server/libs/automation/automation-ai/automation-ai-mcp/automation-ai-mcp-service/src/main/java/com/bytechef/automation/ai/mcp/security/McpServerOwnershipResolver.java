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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class McpServerOwnershipResolver implements ResourceOwnershipResolver {

    private final McpServerRepository mcpServerRepository;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpServerOwnershipResolver(
        McpServerRepository mcpServerRepository, WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpServerRepository = mcpServerRepository;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpServer";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<McpServer> mcpServer = mcpServerRepository.findById(id);

        if (mcpServer.isEmpty() || mcpServer.get()
            .getType() != PlatformType.AUTOMATION) {

            return ResourceOwner.unknown();
        }

        Optional<Long> workspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(id);

        return workspaceId.map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}

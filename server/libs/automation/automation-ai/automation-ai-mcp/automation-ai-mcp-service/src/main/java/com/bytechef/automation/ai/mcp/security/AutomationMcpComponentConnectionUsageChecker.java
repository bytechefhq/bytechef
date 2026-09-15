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
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentConnectionUsageChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Checks a connection bound to an MCP component through {@link PermissionService#canUseConnectionInWorkspace}, in the
 * workspace and environment of the component's MCP server.
 *
 * @author Ivica Cardic
 */
@Component
public class AutomationMcpComponentConnectionUsageChecker implements McpComponentConnectionUsageChecker {

    private final McpServerRepository mcpServerRepository;
    private final PermissionService permissionService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpComponentConnectionUsageChecker(
        McpServerRepository mcpServerRepository, @Lazy PermissionService permissionService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpServerRepository = mcpServerRepository;
        this.permissionService = permissionService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public void checkConnectionUsage(long mcpServerId, long connectionId) {
        if (!canUseConnection(mcpServerId, connectionId)) {
            throw new AccessDeniedException(
                "Connection id=%s cannot be used by MCP server id=%s".formatted(connectionId, mcpServerId));
        }
    }

    private boolean canUseConnection(long mcpServerId, long connectionId) {
        Optional<Environment> environment = mcpServerRepository.findById(mcpServerId)
            .map(McpServer::getEnvironment);
        Optional<Long> workspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId);

        if (environment.isEmpty() || workspaceId.isEmpty()) {
            return permissionService.isTenantAdmin();
        }

        return permissionService.canUseConnectionInWorkspace(connectionId, workspaceId.get(), environment.get());
    }
}

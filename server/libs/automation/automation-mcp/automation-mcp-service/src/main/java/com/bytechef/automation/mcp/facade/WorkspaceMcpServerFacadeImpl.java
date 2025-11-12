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

package com.bytechef.automation.mcp.facade;

import com.bytechef.automation.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceMcpServerFacade} interface that handles workspace MCP server operations.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceMcpServerFacadeImpl implements WorkspaceMcpServerFacade {

    private final McpServerService mcpServerService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public WorkspaceMcpServerFacadeImpl(
        McpServerService mcpServerService, WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpServerService = mcpServerService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServer> getWorkspaceMcpServers(Long workspaceId) {
        List<WorkspaceMcpServer> workspaceMcpServers = workspaceMcpServerService.getWorkspaceMcpServers(workspaceId);

        return workspaceMcpServers.stream()
            .map(workspaceMcpServer -> mcpServerService.getMcpServer(workspaceMcpServer.getMcpServerId()))
            .toList();
    }

    @Override
    public McpServer createWorkspaceMcpServer(
        String name, ModeType type, Environment environment, Boolean enabled, Long workspaceId) {

        McpServer mcpServer = mcpServerService.create(name, type, environment, enabled);

        workspaceMcpServerService.assignMcpServerToWorkspace(mcpServer.getId(), workspaceId);

        return mcpServer;
    }

    @Override
    public void deleteWorkspaceMcpServer(Long mcpServerId) {
        workspaceMcpServerService.removeMcpServerFromWorkspace(mcpServerId);

        mcpServerService.delete(mcpServerId);
    }
}

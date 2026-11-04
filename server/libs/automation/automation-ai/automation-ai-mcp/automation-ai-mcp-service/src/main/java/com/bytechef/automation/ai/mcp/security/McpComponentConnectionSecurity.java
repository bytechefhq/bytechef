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
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("mcpComponentConnectionSecurity")
public class McpComponentConnectionSecurity {

    private final ConnectionService connectionService;
    private final McpComponentRepository mcpComponentRepository;
    private final McpServerRepository mcpServerRepository;
    private final WorkspaceConnectionService workspaceConnectionService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpComponentConnectionSecurity(
        ConnectionService connectionService, McpComponentRepository mcpComponentRepository,
        McpServerRepository mcpServerRepository, WorkspaceConnectionService workspaceConnectionService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.connectionService = connectionService;
        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.workspaceConnectionService = workspaceConnectionService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    public boolean isMcpComponentConnection(long mcpComponentId, long connectionId) {
        Optional<McpComponent> mcpComponentOptional = mcpComponentRepository.findById(mcpComponentId);

        if (mcpComponentOptional.isEmpty()) {
            return false;
        }

        McpComponent mcpComponent = mcpComponentOptional.get();

        Long mcpServerId = mcpComponent.getMcpServerId();

        Optional<Long> workspaceIdOptional = workspaceMcpServerService.fetchMcpServerWorkspaceId(mcpServerId);
        Optional<McpServer> mcpServerOptional = mcpServerRepository.findById(mcpServerId);

        if (workspaceIdOptional.isEmpty() || mcpServerOptional.isEmpty()) {
            return false;
        }

        List<WorkspaceConnection> workspaceConnections = workspaceConnectionService.getWorkspaceConnections(
            workspaceIdOptional.get());

        boolean workspaceConnection = workspaceConnections.stream()
            .anyMatch(curWorkspaceConnection -> Objects.equals(curWorkspaceConnection.getConnectionId(), connectionId));

        if (!workspaceConnection) {
            return false;
        }

        McpServer mcpServer = mcpServerOptional.get();

        Connection connection = connectionService.getConnection(connectionId);

        return connection.getEnvironmentId() == mcpServer.getEnvironmentId();
    }
}

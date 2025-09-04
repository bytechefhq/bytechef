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

package com.bytechef.automation.mcp.service;

import com.bytechef.automation.mcp.domain.WorkspaceMcpServer;
import java.util.List;

/**
 * Service interface for managing workspace MCP server relationships.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceMcpServerService {

    /**
     * Gets MCP servers filtered by workspace ID.
     *
     * @param workspaceId the workspace ID to filter by
     * @return a list of MCP servers in the specified workspace
     */
    List<WorkspaceMcpServer> getWorkspaceMcpServers(Long workspaceId);

    /**
     * Assigns an MCP server to a workspace.
     *
     * @param mcpServerId the MCP server ID
     * @param workspaceId the workspace ID
     */
    void assignMcpServerToWorkspace(Long mcpServerId, Long workspaceId);

    /**
     * Removes an MCP server from a workspace.
     *
     * @param mcpServerId the MCP server ID
     */
    void removeMcpServerFromWorkspace(Long mcpServerId);
}

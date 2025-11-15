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

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;

/**
 * Defines the interface for managing and retrieving MCP servers associated with specific workspaces. This facade
 * abstracts the details of underlying services and provides a streamlined way to access MCP server information tied to
 * a workspace.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceMcpServerFacade {

    /**
     * Retrieves a list of MCP servers associated with the specified workspace.
     *
     * @param workspaceId the unique identifier of the workspace for which the MCP servers are to be retrieved
     * @return a list of {@code McpServer} objects associated with the workspace
     */
    List<McpServer> getWorkspaceMcpServers(Long workspaceId);

    /**
     * Creates a new MCP server and assigns it to the specified workspace.
     *
     * @param name        the name of the MCP server
     * @param type        the type of the MCP server
     * @param environment the environment of the MCP server
     * @param enabled     whether the server is enabled (can be null for default value)
     * @param workspaceId the workspace ID to assign the server to
     * @return the created MCP server
     */
    McpServer createWorkspaceMcpServer(
        String name, ModeType type, Environment environment, Boolean enabled, Long workspaceId);

    /**
     * Deletes an MCP server and removes it from all workspaces.
     *
     * @param mcpServerId the ID of the MCP server to delete
     */
    void deleteWorkspaceMcpServer(Long mcpServerId);
}

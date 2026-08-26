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

package com.bytechef.automation.ai.mcp.facade;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
     * Retrieves the distinct tags assigned to the MCP servers of the specified workspace.
     *
     * @param workspaceId the unique identifier of the workspace whose MCP server tags are to be retrieved
     * @return a list of {@code Tag} objects assigned to the workspace's MCP servers
     */
    List<Tag> getWorkspaceMcpServerTags(Long workspaceId);

    /**
     * Retrieves the MCP projects belonging to the MCP servers of the specified workspace.
     *
     * @param workspaceId the unique identifier of the workspace whose MCP projects are to be retrieved
     * @return a list of {@code McpProject} objects belonging to the workspace's MCP servers
     */
    List<McpProject> getWorkspaceMcpProjects(Long workspaceId);

    /**
     * Creates a new MCP server and assigns it to the specified workspace.
     *
     * @param name                   the name of the MCP server
     * @param type                   the type of the MCP server
     * @param environment            the environment of the MCP server
     * @param enabled                whether the server is enabled (can be null for default value)
     * @param authenticationRequired whether callers must present an API key in addition to the server secret (can be
     *                               null for the domain default, which is required)
     * @param workspaceId            the workspace ID to assign the server to
     * @return the created MCP server
     */
    McpServer createWorkspaceMcpServer(
        String name, PlatformType type, Environment environment, Boolean enabled, Boolean authenticationRequired,
        Long workspaceId);

    /**
     * Creates a new MCP server and assigns it to the specified workspace.
     *
     * @param name                     the name of the MCP server
     * @param type                     the type of the MCP server
     * @param environment              the environment of the MCP server
     * @param enabled                  whether the server is enabled (can be null for default value)
     * @param authenticationRequired   whether callers must present an API key in addition to the server secret (can be
     *                                 null for the domain default, which is required)
     * @param enforceToolAuthorization whether individual tool calls must additionally be authorized (can be null for
     *                                 the domain default)
     * @param workspaceId              the workspace ID to assign the server to
     * @param uuid                     the cross-environment lineage identifier to assign to the server (can be null to
     *                                 generate a new one)
     * @return the created MCP server
     */
    McpServer createWorkspaceMcpServer(
        String name, PlatformType type, Environment environment, Boolean enabled, Boolean authenticationRequired,
        Boolean enforceToolAuthorization, Long workspaceId, @Nullable UUID uuid);

    /**
     * Updates the name and/or enabled state of an MCP server belonging to a workspace.
     *
     * @param mcpServerId the ID of the MCP server to update
     * @param name        the new name (null to leave unchanged)
     * @param enabled     the new enabled state (null to leave unchanged)
     * @return the updated MCP server
     */
    McpServer updateWorkspaceMcpServer(Long mcpServerId, String name, Boolean enabled);

    /**
     * Deletes an MCP server and removes it from all workspaces.
     *
     * @param mcpServerId the ID of the MCP server to delete
     */
    void deleteWorkspaceMcpServer(Long mcpServerId);
}

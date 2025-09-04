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

package com.bytechef.automation.mcp.repository;

import com.bytechef.automation.mcp.domain.WorkspaceMcpServer;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository interface for managing {@link WorkspaceMcpServer} entities.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceMcpServerRepository extends ListCrudRepository<WorkspaceMcpServer, Long> {

    /**
     * Find all workspace MCP server relationships by workspace ID.
     *
     * @param workspaceId the workspace ID
     * @return list of workspace MCP server relationships
     */
    List<WorkspaceMcpServer> findAllByWorkspaceId(Long workspaceId);

    /**
     * Find all workspace MCP server relationships by MCP server ID.
     *
     * @param mcpServerId the MCP server ID
     * @return list of workspace MCP server relationships
     */
    List<WorkspaceMcpServer> findByMcpServerId(Long mcpServerId);

    /**
     * Find workspace MCP server relationship by workspace ID and MCP server ID.
     *
     * @param workspaceId the workspace ID
     * @param mcpServerId the MCP server ID
     * @return workspace MCP server relationship if found
     */
    WorkspaceMcpServer findByWorkspaceIdAndMcpServerId(Long workspaceId, Long mcpServerId);
}

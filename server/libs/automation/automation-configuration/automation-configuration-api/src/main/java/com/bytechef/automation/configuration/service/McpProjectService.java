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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.McpProject;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpProject} entities.
 *
 * @author Ivica Cardic
 */
public interface McpProjectService {

    /**
     * Creates a new MCP project.
     *
     * @param mcpProject the MCP project to create
     * @return the created MCP project
     */
    McpProject create(McpProject mcpProject);

    /**
     * Updates an existing MCP project.
     *
     * @param mcpProject the MCP project to update
     * @return the updated MCP project
     */
    McpProject update(McpProject mcpProject);

    /**
     * Deletes an MCP project by ID.
     *
     * @param mcpProjectId the ID of the MCP project to delete
     */
    void delete(long mcpProjectId);

    /**
     * Fetches an MCP project by ID.
     *
     * @param mcpProjectId the ID of the MCP project to fetch
     * @return the MCP project, or empty if not found
     */
    Optional<McpProject> fetchMcpProject(long mcpProjectId);

    /**
     * Gets all MCP projects.
     *
     * @return a list of all MCP projects
     */
    List<McpProject> getMcpProjects();

    /**
     * Gets all MCP projects for a specific MCP server.
     *
     * @param mcpServerId the ID of the MCP server
     * @return a list of MCP projects for the specified server
     */
    List<McpProject> getMcpServerMcpProjects(long mcpServerId);
}

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

package com.bytechef.platform.configuration.service;

import com.bytechef.platform.configuration.domain.McpTool;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpTool} entities.
 *
 * @author Ivica Cardic
 */
public interface McpToolService {

    /**
     * Creates a new MCP tool.
     *
     * @param mcpTool the MCP tool to create
     * @return the created MCP tool
     */
    McpTool create(McpTool mcpTool);

    /**
     * Updates an existing MCP tool.
     *
     * @param mcpTool the MCP tool to update
     * @return the updated MCP tool
     */
    McpTool update(McpTool mcpTool);

    /**
     * Deletes an MCP tool by ID.
     *
     * @param mcpToolId the ID of the MCP tool to delete
     */
    void delete(long mcpToolId);

    /**
     * Fetches an MCP tool by ID.
     *
     * @param mcpToolId the ID of the MCP tool to fetch
     * @return the MCP tool, or empty if not found
     */
    Optional<McpTool> fetchMcpTool(long mcpToolId);

    /**
     * Gets all MCP tools.
     *
     * @return a list of all MCP tools
     */
    List<McpTool> getMcpTools();

    /**
     * Gets all MCP tools for a specific MCP component.
     *
     * @param mcpComponentId the ID of the MCP component
     * @return a list of MCP tools for the specified component
     */
    List<McpTool> getMcpComponentMcpTools(long mcpComponentId);
}

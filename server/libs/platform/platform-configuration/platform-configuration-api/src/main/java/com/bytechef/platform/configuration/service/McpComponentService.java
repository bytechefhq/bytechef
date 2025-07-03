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

import com.bytechef.platform.configuration.domain.McpComponent;
import java.util.List;

/**
 * Service interface for managing {@link McpComponent} entities.
 *
 * @author Ivica Cardic
 */
public interface McpComponentService {

    /**
     * Creates a new MCP component.
     *
     * @param mcpComponent the MCP component to create
     * @return the created MCP component
     */
    McpComponent create(McpComponent mcpComponent);

    /**
     * Updates an existing MCP component.
     *
     * @param mcpComponent the MCP component to update
     * @return the updated MCP component
     */
    McpComponent update(McpComponent mcpComponent);

    /**
     * Deletes an MCP component by ID.
     *
     * @param mcpComponentId the ID of the MCP component to delete
     */
    void delete(long mcpComponentId);

    /**
     * Retrieves an MCP component by its ID.
     *
     * @param mcpComponentId the ID of the MCP component to retrieve
     * @return the MCP component with the specified ID
     */
    McpComponent getMcpComponent(long mcpComponentId);

    /**
     * Gets all MCP components.
     *
     * @return a list of all MCP components
     */
    List<McpComponent> getMcpComponents();

    /**
     * Gets all MCP components for a specific MCP server.
     *
     * @param mcpServerId the ID of the MCP server
     * @return a list of MCP components for the specified server
     */
    List<McpComponent> getMcpServerMcpComponents(long mcpServerId);
}

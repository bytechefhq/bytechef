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

import com.bytechef.platform.configuration.domain.McpAction;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpAction} entities.
 *
 * @author Ivica Cardic
 */
public interface McpActionService {

    /**
     * Creates a new MCP action.
     *
     * @param mcpAction the MCP action to create
     * @return the created MCP action
     */
    McpAction create(McpAction mcpAction);

    /**
     * Updates an existing MCP action.
     *
     * @param mcpAction the MCP action to update
     * @return the updated MCP action
     */
    McpAction update(McpAction mcpAction);

    /**
     * Deletes an MCP action by ID.
     *
     * @param mcpActionId the ID of the MCP action to delete
     */
    void delete(long mcpActionId);

    /**
     * Fetches an MCP action by ID.
     *
     * @param mcpActionId the ID of the MCP action to fetch
     * @return the MCP action, or empty if not found
     */
    Optional<McpAction> fetchMcpAction(long mcpActionId);

    /**
     * Gets all MCP actions.
     *
     * @return a list of all MCP actions
     */
    List<McpAction> getMcpActions();

    /**
     * Gets all MCP actions for a specific MCP component.
     *
     * @param mcpComponentId the ID of the MCP component
     * @return a list of MCP actions for the specified component
     */
    List<McpAction> getMcpActionsByComponentId(long mcpComponentId);
}

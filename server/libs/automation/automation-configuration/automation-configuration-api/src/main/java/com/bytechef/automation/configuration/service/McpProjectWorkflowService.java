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

import com.bytechef.platform.configuration.domain.McpProjectWorkflow;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpProjectWorkflow} entities.
 *
 * @author Ivica Cardic
 */
public interface McpProjectWorkflowService {

    /**
     * Creates a new MCP project workflow.
     *
     * @param mcpProjectWorkflow the MCP project workflow to create
     * @return the created MCP project workflow
     */
    McpProjectWorkflow create(McpProjectWorkflow mcpProjectWorkflow);

    /**
     * Creates a new MCP project workflow from the provided input parameters.
     *
     * @param mcpProjectId                the ID of the MCP project
     * @param projectDeploymentWorkflowId the ID of the project deployment workflow
     * @return the created MCP project workflow
     */
    McpProjectWorkflow create(Long mcpProjectId, Long projectDeploymentWorkflowId);

    /**
     * Deletes an MCP project workflow by ID.
     *
     * @param mcpProjectWorkflowId the ID of the MCP project workflow to delete
     */
    void delete(long mcpProjectWorkflowId);

    /**
     * Fetches an MCP project workflow by ID.
     *
     * @param mcpProjectWorkflowId the ID of the MCP project workflow to fetch
     * @return the MCP project workflow, or empty if not found
     */
    Optional<McpProjectWorkflow> fetchMcpProjectWorkflow(long mcpProjectWorkflowId);

    /**
     * Gets all MCP project workflows.
     *
     * @return a list of all MCP project workflows
     */
    List<McpProjectWorkflow> getMcpProjectWorkflows();

    /**
     * Gets MCP project workflows filtered by MCP project ID.
     *
     * @param mcpProjectId the MCP project ID to filter by
     * @return a list of MCP project workflows with the given MCP project ID
     */
    List<McpProjectWorkflow> getMcpProjectMcpProjectWorkflows(Long mcpProjectId);

    /**
     * Gets MCP project workflows filtered by project deployment workflow ID.
     *
     * @param projectDeploymentWorkflowId the project deployment workflow ID to filter by
     * @return a list of MCP project workflows with the given project deployment workflow ID
     */
    List<McpProjectWorkflow> getProjectDeploymentWorkflowMcpProjectWorkflows(Long projectDeploymentWorkflowId);

    /**
     * Updates an existing MCP project workflow.
     *
     * @param mcpProjectWorkflow the MCP project workflow to update
     * @return the updated MCP project workflow
     */
    McpProjectWorkflow update(McpProjectWorkflow mcpProjectWorkflow);

    /**
     * Updates an existing MCP project workflow with the provided input parameters.
     *
     * @param id                          the ID of the MCP project workflow to update
     * @param mcpProjectId                the ID of the MCP project (can be null if not updating)
     * @param projectDeploymentWorkflowId the ID of the project deployment workflow (can be null if not updating)
     * @return the updated MCP project workflow
     * @throws IllegalArgumentException if the MCP project workflow with the given ID is not found
     */
    McpProjectWorkflow update(long id, Long mcpProjectId, Long projectDeploymentWorkflowId);
}

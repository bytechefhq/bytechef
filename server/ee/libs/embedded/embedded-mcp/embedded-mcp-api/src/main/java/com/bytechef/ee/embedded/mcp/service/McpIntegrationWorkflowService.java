/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing {@link McpIntegrationWorkflow} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface McpIntegrationWorkflowService {

    /**
     * Creates a new MCP integration workflow.
     *
     * @param mcpIntegrationWorkflow the MCP integration workflow to create
     * @return the created MCP integration workflow
     */
    McpIntegrationWorkflow create(McpIntegrationWorkflow mcpIntegrationWorkflow);

    /**
     * Creates a new MCP integration workflow from the provided input parameters.
     *
     * @param mcpIntegrationId                           the ID of the MCP integration
     * @param integrationInstanceConfigurationWorkflowId the ID of the integration instance configuration workflow
     * @return the created MCP integration workflow
     */
    McpIntegrationWorkflow create(Long mcpIntegrationId, Long integrationInstanceConfigurationWorkflowId);

    /**
     * Deletes an MCP integration workflow by ID.
     *
     * @param mcpIntegrationWorkflowId the ID of the MCP integration workflow to delete
     */
    void delete(long mcpIntegrationWorkflowId);

    /**
     * Fetches an MCP integration workflow by ID.
     *
     * @param mcpIntegrationWorkflowId the ID of the MCP integration workflow to fetch
     * @return the MCP integration workflow, or empty if not found
     */
    Optional<McpIntegrationWorkflow> fetchMcpIntegrationWorkflow(long mcpIntegrationWorkflowId);

    /**
     * Gets MCP integration workflows filtered by MCP integration ID.
     *
     * @param mcpIntegrationId the MCP integration ID to filter by
     * @return a list of MCP integration workflows with the given MCP integration ID
     */
    List<McpIntegrationWorkflow> getMcpIntegrationMcpIntegrationWorkflows(Long mcpIntegrationId);

    /**
     * Gets all MCP integration workflows.
     *
     * @return a list of all MCP integration workflows
     */
    List<McpIntegrationWorkflow> getMcpIntegrationWorkflows();

    /**
     * Updates an existing MCP integration workflow.
     *
     * @param mcpIntegrationWorkflow the MCP integration workflow to update
     * @return the updated MCP integration workflow
     */
    McpIntegrationWorkflow update(McpIntegrationWorkflow mcpIntegrationWorkflow);

    /**
     * Updates an existing MCP integration workflow with the provided input parameters.
     *
     * @param id                                         the ID of the MCP integration workflow to update
     * @param mcpIntegrationId                           the ID of the MCP integration (can be null if not updating)
     * @param integrationInstanceConfigurationWorkflowId the ID of the integration instance configuration workflow (can
     *                                                   be null if not updating)
     * @return the updated MCP integration workflow
     * @throws IllegalArgumentException if the MCP integration workflow with the given ID is not found
     */
    McpIntegrationWorkflow update(long id, Long mcpIntegrationId, Long integrationInstanceConfigurationWorkflowId);

    /**
     * Updates the parameters of an existing MCP integration workflow.
     *
     * @param id         the ID of the MCP integration workflow to update
     * @param parameters the parameters to set
     * @return the updated MCP integration workflow
     */
    McpIntegrationWorkflow updateParameters(long id, Map<String, ?> parameters);
}

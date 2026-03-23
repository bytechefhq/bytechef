/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing {@link McpIntegrationInstanceConfigurationWorkflow} entities.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceConfigurationWorkflowService {

    /**
     * Creates a new MCP integration workflow.
     *
     * @param mcpIntegrationInstanceConfigurationWorkflow the MCP integration workflow to create
     * @return the created MCP integration workflow
     */
    McpIntegrationInstanceConfigurationWorkflow
        create(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow);

    /**
     * Creates a new MCP integration workflow from the provided input parameters.
     *
     * @param mcpIntegrationInstanceConfigurationId      the ID of the MCP integration
     * @param integrationInstanceConfigurationWorkflowId the ID of the integration instance configuration workflow
     * @return the created MCP integration workflow
     */
    McpIntegrationInstanceConfigurationWorkflow
        create(Long mcpIntegrationInstanceConfigurationId, Long integrationInstanceConfigurationWorkflowId);

    /**
     * Deletes an MCP integration workflow by ID.
     *
     * @param mcpIntegrationInstanceConfigurationWorkflowId the ID of the MCP integration workflow to delete
     */
    void delete(long mcpIntegrationInstanceConfigurationWorkflowId);

    /**
     * Deletes MCP integration workflows by integration instance configuration workflow ID.
     *
     * @param integrationInstanceConfigurationWorkflowId the ID of the integration instance configuration workflow
     */
    void deleteByIntegrationInstanceConfigurationWorkflowId(long integrationInstanceConfigurationWorkflowId);

    /**
     * Fetches an MCP integration workflow by ID.
     *
     * @param mcpIntegrationInstanceConfigurationWorkflowId the ID of the MCP integration workflow to fetch
     * @return the MCP integration workflow, or empty if not found
     */
    Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId);

    /**
     * Fetches an MCP integration workflow by the integration instance configuration workflow ID.
     *
     * @param integrationInstanceConfigurationWorkflowId the integration instance configuration workflow ID
     * @return the MCP integration workflow, or empty if not found
     */
    Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflowByIntegrationInstanceConfigurationWorkflowId(
            long integrationInstanceConfigurationWorkflowId);

    /**
     * Fetches an MCP integration workflow by the Atlas workflow ID.
     *
     * @param workflowId the Atlas workflow ID
     * @return the MCP integration workflow, or empty if not found
     */
    Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflowByWorkflowId(String workflowId);

    /**
     * Gets MCP integration workflows filtered by MCP integration ID.
     *
     * @param mcpIntegrationInstanceConfigurationId the MCP integration ID to filter by
     * @return a list of MCP integration workflows with the given MCP integration ID
     */
    List<McpIntegrationInstanceConfigurationWorkflow>
        getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
            Long mcpIntegrationInstanceConfigurationId);

    /**
     * Gets all MCP integration workflows.
     *
     * @return a list of all MCP integration workflows
     */
    List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows();

    /**
     * Updates an existing MCP integration workflow.
     *
     * @param mcpIntegrationInstanceConfigurationWorkflow the MCP integration workflow to update
     * @return the updated MCP integration workflow
     */
    McpIntegrationInstanceConfigurationWorkflow
        update(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow);

    /**
     * Updates an existing MCP integration workflow with the provided input parameters.
     *
     * @param id                                         the ID of the MCP integration workflow to update
     * @param mcpIntegrationInstanceConfigurationId      the ID of the MCP integration (can be null if not updating)
     * @param integrationInstanceConfigurationWorkflowId the ID of the integration instance configuration workflow (can
     *                                                   be null if not updating)
     * @return the updated MCP integration workflow
     * @throws IllegalArgumentException if the MCP integration workflow with the given ID is not found
     */
    McpIntegrationInstanceConfigurationWorkflow
        update(long id, Long mcpIntegrationInstanceConfigurationId, Long integrationInstanceConfigurationWorkflowId);

    /**
     * Updates the parameters of an existing MCP integration workflow.
     *
     * @param id         the ID of the MCP integration workflow to update
     * @param parameters the parameters to set
     * @return the updated MCP integration workflow
     */
    McpIntegrationInstanceConfigurationWorkflow updateParameters(long id, Map<String, ?> parameters);
}

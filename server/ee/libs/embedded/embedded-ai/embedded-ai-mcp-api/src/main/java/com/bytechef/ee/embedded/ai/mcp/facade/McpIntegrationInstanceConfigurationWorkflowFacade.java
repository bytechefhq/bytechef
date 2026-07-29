/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Facade for managing MCP Integration Workflow operations that involve multiple services.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceConfigurationWorkflowFacade {

    /**
     * Deletes an MCP integration workflow and its associated integration instance configuration workflow.
     *
     * @param mcpIntegrationInstanceConfigurationWorkflowId the ID of the MCP integration workflow to delete
     */
    McpIntegrationInstanceConfigurationWorkflow createMcpIntegrationInstanceConfigurationWorkflow(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId);

    void deleteMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId);

    McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflow(
        long id, @Nullable Long mcpIntegrationInstanceConfigurationId,
        @Nullable Long integrationInstanceConfigurationWorkflowId);

    McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflowParameters(
        long id, Map<String, ?> parameters);

    // The following are tenant-admin-gated reads for the management GraphQL surface, keeping the gate on the facade
    // rather than the controller while the underlying services stay ungated for their runtime callers. The properties
    // query stays in the controller (it builds UI Property metadata) but is gated by fetching its subject through
    // getMcpIntegrationInstanceConfigurationWorkflow first.

    @Nullable
    McpIntegrationInstanceConfigurationWorkflow getMcpIntegrationInstanceConfigurationWorkflow(long id);

    List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows();

    List<McpIntegrationInstanceConfigurationWorkflow>
        getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
            long mcpIntegrationInstanceConfigurationId);

    List<IntegrationWorkflowDTO> getToolEligibleIntegrationVersionWorkflows(long integrationId, int integrationVersion);

    List<IntegrationWorkflowDTO> getToolEligibleIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId);
}

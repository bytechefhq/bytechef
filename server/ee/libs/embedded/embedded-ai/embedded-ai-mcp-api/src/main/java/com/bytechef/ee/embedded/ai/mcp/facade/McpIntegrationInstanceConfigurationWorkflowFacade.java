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

    McpIntegrationInstanceConfigurationWorkflow createMcpIntegrationInstanceConfigurationWorkflow(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId);

    void deleteMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId);

    McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflow(
        long id, @Nullable Long mcpIntegrationInstanceConfigurationId,
        @Nullable Long integrationInstanceConfigurationWorkflowId);

    McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflowParameters(
        long id, Map<String, ?> parameters);

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

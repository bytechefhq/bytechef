/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

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
    void deleteMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId);
}

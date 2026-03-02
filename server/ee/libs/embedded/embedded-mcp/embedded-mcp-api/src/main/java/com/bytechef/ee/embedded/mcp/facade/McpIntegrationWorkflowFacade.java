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
 * @author Ivica Cardic
 * @version ee
 */
public interface McpIntegrationWorkflowFacade {

    /**
     * Deletes an MCP integration workflow and its associated integration instance configuration workflow.
     *
     * @param mcpIntegrationWorkflowId the ID of the MCP integration workflow to delete
     */
    void deleteMcpIntegrationWorkflow(long mcpIntegrationWorkflowId);
}

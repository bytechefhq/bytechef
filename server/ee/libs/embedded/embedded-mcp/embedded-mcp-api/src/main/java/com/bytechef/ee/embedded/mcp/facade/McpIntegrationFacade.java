/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import java.util.List;

/**
 * Facade for managing MCP Integration operations that involve multiple services.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface McpIntegrationFacade {

    /**
     * Creates a new MCP integration with workflows, auto-creating an IntegrationInstanceConfiguration.
     *
     * @param mcpServerId         the MCP server ID
     * @param integrationId       the integration ID
     * @param integrationVersion  the integration version
     * @param selectedWorkflowIds the list of selected workflow IDs
     * @return the created MCP integration
     */
    McpIntegration createMcpIntegration(
        long mcpServerId, long integrationId, int integrationVersion, List<String> selectedWorkflowIds);

    /**
     * Deletes an MCP integration identified by its unique ID.
     *
     * @param mcpIntegrationId the unique identifier of the MCP integration to be deleted
     */
    void deleteMcpIntegration(long mcpIntegrationId);
}

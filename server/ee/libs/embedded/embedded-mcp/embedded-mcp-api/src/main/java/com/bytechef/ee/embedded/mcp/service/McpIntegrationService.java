/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpIntegration} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface McpIntegrationService {

    /**
     * Creates a new MCP integration.
     *
     * @param mcpIntegration the MCP integration to create
     * @return the created MCP integration
     */
    McpIntegration create(McpIntegration mcpIntegration);

    /**
     * Deletes an MCP integration by ID.
     *
     * @param mcpIntegrationId the ID of the MCP integration to delete
     */
    void delete(long mcpIntegrationId);

    /**
     * Fetches an MCP integration by ID.
     *
     * @param mcpIntegrationId the ID of the MCP integration to fetch
     * @return the MCP integration, or empty if not found
     */
    Optional<McpIntegration> fetchMcpIntegration(long mcpIntegrationId);

    /**
     * Gets all MCP integrations.
     *
     * @return a list of all MCP integrations
     */
    List<McpIntegration> getMcpIntegrations();

    /**
     * Gets all MCP integrations for a specific MCP server.
     *
     * @param mcpServerId the ID of the MCP server
     * @return a list of MCP integrations for the specified server
     */
    List<McpIntegration> getMcpServerMcpIntegrations(long mcpServerId);

    /**
     * Updates an existing MCP integration.
     *
     * @param mcpIntegration the MCP integration to update
     * @return the updated MCP integration
     */
    McpIntegration update(McpIntegration mcpIntegration);
}

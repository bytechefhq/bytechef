/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpIntegrationInstanceConfiguration} entities.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceConfigurationService {

    /**
     * Creates a new MCP integration.
     *
     * @param mcpIntegrationInstanceConfiguration the MCP integration to create
     * @return the created MCP integration
     */
    McpIntegrationInstanceConfiguration create(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration);

    /**
     * Deletes an MCP integration by ID.
     *
     * @param mcpIntegrationInstanceConfigurationId the ID of the MCP integration to delete
     */
    void delete(long mcpIntegrationInstanceConfigurationId);

    /**
     * Deletes all MCP integrations associated with a specific integration instance configuration.
     *
     * @param integrationInstanceConfigurationId the ID of the integration instance configuration
     */
    void deleteByIntegrationInstanceConfigurationId(long integrationInstanceConfigurationId);

    /**
     * Fetches an MCP integration by ID.
     *
     * @param mcpIntegrationInstanceConfigurationId the ID of the MCP integration to fetch
     * @return the MCP integration, or empty if not found
     */
    Optional<McpIntegrationInstanceConfiguration>
        fetchMcpIntegrationInstanceConfiguration(long mcpIntegrationInstanceConfigurationId);

    /**
     * Gets all MCP integrations for a specific integration.
     *
     * @param integrationId the ID of the integration
     * @return a list of MCP integrations for the specified integration
     */
    List<McpIntegrationInstanceConfiguration>
        getMcpIntegrationInstanceConfigurationsByIntegrationId(long integrationId);

    /**
     * Gets all MCP integrations for a specific integration instance configuration.
     *
     * @param integrationInstanceConfigurationId the ID of the integration instance configuration
     * @return a list of MCP integrations for the specified integration instance configuration
     */
    List<McpIntegrationInstanceConfiguration>
        getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
            long integrationInstanceConfigurationId);

    /**
     * Gets all MCP integrations.
     *
     * @return a list of all MCP integrations
     */
    List<McpIntegrationInstanceConfiguration> getMcpIntegrationInstanceConfigurations();

    /**
     * Gets all MCP integrations for a specific MCP server.
     *
     * @param mcpServerId the ID of the MCP server
     * @return a list of MCP integrations for the specified server
     */
    List<McpIntegrationInstanceConfiguration> getMcpServerMcpIntegrationInstanceConfigurations(long mcpServerId);

    /**
     * Updates an existing MCP integration.
     *
     * @param mcpIntegrationInstanceConfiguration the MCP integration to update
     * @return the updated MCP integration
     */
    McpIntegrationInstanceConfiguration update(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration);
}

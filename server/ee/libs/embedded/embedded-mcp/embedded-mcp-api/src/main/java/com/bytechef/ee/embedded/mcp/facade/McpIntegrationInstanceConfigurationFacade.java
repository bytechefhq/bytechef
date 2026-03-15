/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import java.util.List;

/**
 * Facade for managing MCP Integration operations that involve multiple services.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface McpIntegrationInstanceConfigurationFacade {

    /**
     * Creates a new MCP integration instance configuration linked to an existing IntegrationInstanceConfiguration. For
     * each selected workflow, reuses an existing IntegrationInstanceConfigurationWorkflow if one already exists
     * (enabling it if disabled), or creates a new one otherwise.
     *
     * @param mcpServerId                        the MCP server ID
     * @param integrationInstanceConfigurationId the existing integration instance configuration ID
     * @param selectedWorkflowIds                the list of selected workflow IDs
     * @return the created McpIntegrationInstanceConfiguration
     */
    McpIntegrationInstanceConfiguration createMcpIntegrationInstanceConfiguration(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds);

    /**
     * Deletes an MCP integration instance configuration and its associated MCP workflow records. The underlying
     * IntegrationInstanceConfiguration entity is preserved, but its IntegrationInstanceConfigurationWorkflows are
     * disabled and their IntegrationInstanceWorkflow records are deleted.
     *
     * @param mcpIntegrationInstanceConfigurationId the unique identifier of the MCP integration to be deleted
     */
    void deleteMcpIntegrationInstanceConfiguration(long mcpIntegrationInstanceConfigurationId);

    /**
     * Updates the selected workflows of an MCP integration by computing a diff against the current state. New workflows
     * are created, deselected workflows are removed, unchanged workflows are preserved.
     *
     * @param mcpIntegrationInstanceConfigurationId the unique identifier of the MCP integration to update
     * @param selectedWorkflowIds                   the new list of selected workflow IDs
     * @return the updated McpIntegrationInstanceConfiguration
     */
    McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        long mcpIntegrationInstanceConfigurationId, List<String> selectedWorkflowIds);

    /**
     * Updates the integration version of an MCP integration. Deletes MCP integration workflow records that reference
     * old integration instance configuration workflows before delegating the version update to the configuration layer,
     * then re-creates MCP integration workflow records for the new version's workflows.
     *
     * @param mcpIntegrationInstanceConfigurationId the unique identifier of the MCP integration to update
     * @param integrationVersion                    the new integration version
     * @param workflowUuids                         the workflow UUIDs to include in the new version
     */
    void updateMcpIntegrationInstanceConfigurationVersion(
        long mcpIntegrationInstanceConfigurationId, int integrationVersion, List<String> workflowUuids);
}

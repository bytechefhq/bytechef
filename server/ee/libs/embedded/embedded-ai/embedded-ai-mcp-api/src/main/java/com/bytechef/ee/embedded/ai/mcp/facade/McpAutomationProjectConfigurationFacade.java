/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationWorkflowConfigurationDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade for attaching automation project configuration workflows to an embedded MCP server. Attaching or changing a
 * link never changes the automation project configuration itself.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpAutomationProjectConfigurationFacade {

    /**
     * Links an automation project configuration to an MCP server. The configuration must be in the server's environment
     * and every selected workflow must be turned on in it and have the workflow call trigger at the configuration's
     * project version; nothing is persisted when any check fails.
     *
     * @param mcpServerId                      the MCP server ID
     * @param automationProjectConfigurationId the automation project configuration ID
     * @param workflowUuids                    the selected workflow UUIDs
     * @param workflowConfigurations           the tool settings of the selected workflows
     * @return the created link
     */
    McpAutomationProjectConfiguration createMcpAutomationProjectConfiguration(
        long mcpServerId, long automationProjectConfigurationId, List<String> workflowUuids,
        List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations);

    void deleteMcpAutomationProjectConfiguration(long id);

    /**
     * Removes one workflow tool from an MCP automation project configuration.
     *
     * @param id the link workflow ID
     */
    void deleteMcpAutomationProjectConfigurationWorkflow(long id);

    /**
     * Replaces the selected workflows of a link by computing a diff against its current workflows. Newly selected
     * workflows are validated like on creation; unchanged ones are preserved.
     *
     * @param id                     the link ID
     * @param workflowUuids          the new list of selected workflow UUIDs
     * @param workflowConfigurations the tool settings of the selected workflows
     * @return the updated link
     */
    McpAutomationProjectConfiguration updateMcpAutomationProjectConfiguration(
        long id, List<String> workflowUuids, List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations);

    /**
     * Returns the workflow of a link workflow at the pinned project version of its automation project configuration, or
     * empty when the link workflow or the workflow at that version no longer exists.
     *
     * @param mcpAutomationProjectConfigurationWorkflowId the link workflow ID
     * @return the pinned workflow
     */
    Optional<Workflow> fetchMcpAutomationProjectConfigurationWorkflowPinnedWorkflow(
        long mcpAutomationProjectConfigurationWorkflowId);

    /**
     * Returns the workflow of an automation project configuration at the configuration's project version when it has
     * the workflow call trigger, so its tool properties can be configured before it is added to an MCP server.
     *
     * @param automationProjectConfigurationId the automation project configuration ID
     * @param workflowUuid                     the workflow UUID
     * @return the tool workflow, or empty when the workflow does not exist at that version or cannot be a tool
     */
    Optional<Workflow> fetchAutomationProjectConfigurationToolWorkflow(
        long automationProjectConfigurationId, String workflowUuid);

    McpAutomationProjectConfigurationDTO getMcpAutomationProjectConfiguration(long id);

    List<McpAutomationProjectConfigurationDTO> getMcpServerMcpAutomationProjectConfigurations(long mcpServerId);

    /**
     * Returns the UUIDs of the workflows of an automation project configuration that have the workflow call trigger at
     * the configuration's project version, so they can become MCP tools once turned on in the configuration.
     *
     * @param automationProjectConfigurationId the automation project configuration ID
     * @return the workflow UUIDs
     */
    List<String> getToolWorkflowUuids(long automationProjectConfigurationId);

    McpAutomationProjectConfigurationWorkflow updateMcpAutomationProjectConfigurationWorkflowEnabled(
        long id, boolean enabled);

    McpAutomationProjectConfigurationWorkflow updateMcpAutomationProjectConfigurationWorkflowParameters(
        long id, Map<String, ?> parameters);
}

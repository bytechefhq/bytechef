/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.ee.embedded.ai.tool.model.IntegrationWorkflowInfo;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ReadIntegrationWorkflowTools {

    private final IntegrationWorkflowTools delegate;

    @SuppressFBWarnings("EI")
    public ReadIntegrationWorkflowTools(IntegrationWorkflowTools integrationWorkflowTools) {
        this.delegate = integrationWorkflowTools;
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed information including workflow_id, integration_workflow_id, name, description, version, definition, created date, last modified date.")
    public IntegrationWorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId) {
        return delegate.getWorkflow(workflowId);
    }

    @Tool(
        description = "List all workflows in an integration. Returns a list of workflows with their basic information including workflow_id, integration_workflow_id, name and description")
    public List<IntegrationWorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the integration") long integrationId) {
        return delegate.listWorkflows(integrationId);
    }

    @Tool(
        description = "Full-text search across integration workflows. Returns a list of workflows matching the search query in name or description.")
    public List<IntegrationWorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the integration") Long integrationId) {
        return delegate.searchWorkflows(query, integrationId);
    }
}

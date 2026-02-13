/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.tool.automation.impl;

import com.bytechef.ai.mcp.tool.automation.api.ProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.api.WorkflowInfo;
import com.bytechef.ai.mcp.tool.automation.api.WorkflowValidationResult;
import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Read-only project workflow tools implementation that delegates to ProjectWorkflowTools.
 *
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
@Component
@ConditionalOnAiEnabled
public class ReadProjectWorkflowToolsImpl {

    private final ProjectWorkflowTools delegate;

    @SuppressFBWarnings("EI")
    public ReadProjectWorkflowToolsImpl(ProjectWorkflowTools projectTools) {
        this.delegate = projectTools;
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId) {
        return delegate.getWorkflow(workflowId);
    }

    @Tool(description = "Instructions for building workflows")
    public String getWorkflowBuildInstructions() {
        return delegate.getWorkflowBuildInstructions();
    }

    @Tool(
        description = "List all workflows in a project. Returns a list of workflows with their basic information including id, name and description")
    public List<WorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the project") long projectId) {
        return delegate.listWorkflows(projectId);
    }

    @Tool(
        description = "Full-text search across workflows in projects. Returns a list of workflows matching the search query in name or description.")
    public List<WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the project") Long projectId) {
        return delegate.searchWorkflows(query, projectId);
    }

    @Tool(
        description = "Validate a workflow configuration by checking its structure, properties and outputs against the task definitions. Returns validation results with any errors found")
    public WorkflowValidationResult validateWorkflow(
        @ToolParam(description = "The JSON string of the workflow to validate") String workflowId) {
        return delegate.validateWorkflow(workflowId);
    }
}

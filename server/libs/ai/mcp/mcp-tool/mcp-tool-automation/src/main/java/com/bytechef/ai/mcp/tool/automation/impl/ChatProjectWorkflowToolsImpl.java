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

import com.bytechef.ai.mcp.tool.automation.api.ChatProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.api.ProjectWorkflowTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The ProjectWorkflowTools class provides utility methods and components to facilitate the management and execution of
 * project workflows.
 *
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
@Component
public class ChatProjectWorkflowToolsImpl implements ChatProjectWorkflowTools {

    private static final Logger logger = LoggerFactory.getLogger(ChatProjectWorkflowToolsImpl.class);

    private final ProjectWorkflowTools delegate;

    @SuppressFBWarnings("EI")
    public ChatProjectWorkflowToolsImpl(ProjectWorkflowTools projectTools) {
        this.delegate = projectTools;
    }

    @Override
    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public ProjectWorkflowToolsImpl.WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId) {
        return delegate.getWorkflow(workflowId);
    }

    @Override
    @Tool(description = "Instructions for building workflows")
    public String getWorkflowBuildInstructions() {
        return delegate.getWorkflowBuildInstructions();
    }

    @Override
    @Tool(
        description = "List all workflows in a project. Returns a list of workflows with their basic information including id, name and description")
    public List<ProjectWorkflowToolsImpl.WorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the project") long projectId) {
        return delegate.listWorkflows(projectId);
    }

    @Override
    @Tool(
        description = "Full-text search across workflows in projects. Returns a list of workflows matching the search query in name or description.")
    public List<ProjectWorkflowToolsImpl.WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the project") Long projectId) {
        return delegate.searchWorkflows(query, projectId);
    }

    @Override
    @Tool(
        description = "Validate a workflow configuration by checking its structure, properties and outputs against the task definitions. Returns validation results with any errors found")
    public ProjectWorkflowToolsImpl.WorkflowValidationResult validateWorkflow(
        @ToolParam(description = "The JSON string of the workflow to validate") String workflowId) {
        return delegate.validateWorkflow(workflowId);
    }
}

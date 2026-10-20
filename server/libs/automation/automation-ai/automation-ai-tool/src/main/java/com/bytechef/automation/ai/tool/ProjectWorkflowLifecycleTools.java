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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.ai.tool.model.ProjectWorkflowInfo;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Project workflow tools for surfaces that delegate workflow content to the {@code buildWorkflow} intelligent tool:
 * reads, lifecycle writes, and an empty-only create. Nothing here writes a workflow definition, so
 * {@code buildWorkflow} stays the only path to workflow content. Delegates to {@link ProjectWorkflowTools}, which the
 * workflow-authoring subagents register whole.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectWorkflowLifecycleTools {

    private final ProjectWorkflowTools delegate;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowLifecycleTools(ProjectWorkflowTools projectWorkflowTools) {
        this.delegate = projectWorkflowTools;
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId,
        ToolContext toolContext) {

        return delegate.getWorkflow(workflowId, toolContext);
    }

    @Tool(
        description = "List all workflows in a project. Returns a list of workflows with their basic information including id, name and description. Pass a query to return only the workflows whose name or description contains it.")
    public List<WorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the project") long projectId,
        @ToolParam(
            required = false,
            description = "Optional text to match against workflow names and descriptions, case-insensitively. Omit it to list every workflow of the project.") @Nullable String query,
        ToolContext toolContext) {

        return delegate.listWorkflows(projectId, query, toolContext);
    }

    @Tool(
        description = "Full-text search across workflows in projects. Returns a list of workflows matching the search query in name or description.")
    public List<WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the project") Long projectId,
        @ToolParam(
            required = false, description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) Long workspaceId,
        ToolContext toolContext) {

        return delegate.searchWorkflows(query, projectId, workspaceId, toolContext);
    }

    @Tool(
        description = "Create a new, empty workflow (a manual trigger and no tasks) in a ByteChef project. Its content is authored separately. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(description = "The name of the workflow") String name,
        @ToolParam(required = false, description = "A short description of the workflow") @Nullable String description,
        ToolContext toolContext) {

        return delegate.createProjectWorkflow(projectId, emptyDefinition(name, description), toolContext);
    }

    @Tool(description = "Delete a workflow. Returns a confirmation message.")
    public String deleteWorkflow(
        @ToolParam(description = "The ID of the workflow to delete") String workflowId,
        ToolContext toolContext) {

        return delegate.deleteWorkflow(workflowId, toolContext);
    }

    @Tool(
        description = "Bind a connection the user picked to a workflow node so the workflow can be test-run. Call this AFTER the workflow exists AND after the user picks an existing connection (selectConnection) or creates one (createConnection). The chosen connection instance is stored separately, per environment, by this tool. The connection is bound for the environment the user is currently working in. Returns a confirmation message.")
    public String saveWorkflowTestConnection(
        @ToolParam(description = "The id of the workflow the node belongs to") String workflowId,
        @ToolParam(
            description = "The workflow node (task or trigger) name the connection is for, e.g. 'sendChannelMessage_1'") String workflowNodeName,
        @ToolParam(
            description = "The connection key declared in the node's 'connections' block — usually the component name, e.g. 'slack'") String connectionKey,
        @ToolParam(description = "The id of the connection the user picked") long connectionId,
        ToolContext toolContext) {

        return delegate.saveWorkflowTestConnection(
            workflowId, workflowNodeName, connectionKey, connectionId, toolContext);
    }

    static String emptyDefinition(String name, @Nullable String description) {
        Map<String, Object> definition = new LinkedHashMap<>();

        definition.put("label", name);
        definition.put("description", description == null ? "" : description);
        definition.put("inputs", List.of());
        definition.put("triggers", List.of(Map.of("label", "Manual", "name", "trigger_1", "type", "manual/v1/manual")));
        definition.put("tasks", List.of());

        return JsonUtils.write(definition);
    }
}

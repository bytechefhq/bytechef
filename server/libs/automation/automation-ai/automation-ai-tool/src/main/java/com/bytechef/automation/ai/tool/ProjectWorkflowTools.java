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

import static com.bytechef.automation.configuration.exception.ProjectWorkflowErrorType.GET_WORKFLOW;

import com.bytechef.automation.ai.tool.model.ProjectWorkflowInfo;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.exception.ProjectWorkflowErrorType;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The ProjectWorkflowTools class provides utility methods and components to facilitate the management and execution of
 * project workflows.
 *
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
@Component
public class ProjectWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(ProjectWorkflowTools.class);

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "workflowName",
            "description": "workflowDescription",
            "inputs": [],
            "triggers": [
                {
                    "label": "Manual",
                    "name": "trigger_1",
                    "type": "manual/v1/manual"
                }
            ],
            "tasks": []
        }
        """;

    private static final String AGENT_TOOL_ENVIRONMENT_ID_KEY = "bytechef.agentTool.environmentId";
    private static final String PERSISTED_WORKFLOW_CAPTURE_KEY = "bytechef.workflowEditor.persistedWorkflows";
    private static final String DEFAULT_WORKFLOW_NAME = "Workflow";

    private final ComponentDefinitionService componentDefinitionService;
    private final ProjectFacade projectFacade;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ScopedProjectGuard scopedProjectGuard;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowTools(
        ComponentDefinitionService componentDefinitionService, ProjectFacade projectFacade,
        ProjectWorkflowFacade projectWorkflowFacade, ScopedProjectGuard scopedProjectGuard,
        WorkspaceConnectionFacade workspaceConnectionFacade,
        WorkspaceFacade workspaceFacade) {

        this.componentDefinitionService = componentDefinitionService;
        this.projectFacade = projectFacade;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.scopedProjectGuard = scopedProjectGuard;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
        this.workspaceFacade = workspaceFacade;
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId, ToolContext toolContext) {

        scopedProjectGuard.checkWorkflow(workflowId, toolContext, GET_WORKFLOW);

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "getWorkflow({}): Retrieved workflow {}", workflowId, projectWorkflowDTO.getProjectWorkflowId());
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), projectWorkflowDTO.getDefinition(),
                projectWorkflowDTO.getVersion(), projectWorkflowDTO.getCreatedDate(),
                projectWorkflowDTO.getLastModifiedDate());
        } catch (Exception e) {
            log.error("getWorkflow({}): Failed to get workflow {}", workflowId, workflowId, e);

            throw new ExecutionException("Failed to get workflow: " + e.getMessage(), e, GET_WORKFLOW);
        }
    }

    @Tool(
        description = "List all workflows in a project. Returns a list of workflows with their basic information including id, name and description")
    public List<WorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the project") long projectId, ToolContext toolContext) {

        scopedProjectGuard.checkProject(projectId, toolContext, ProjectWorkflowErrorType.LIST_WORKFLOWS);

        try {
            List<ProjectWorkflowDTO> workflows = projectWorkflowFacade.getProjectWorkflows(projectId);

            List<WorkflowInfo> workflowInfos = workflows.stream()
                .map(workflow -> new WorkflowInfo(workflow.getId(),
                    workflow.getProjectWorkflowId(),
                    workflow.getWorkflowUuid(),
                    workflow.getLabel(), workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                    workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                    workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null))
                .toList();

            if (log.isDebugEnabled()) {
                log.debug("listWorkflows({}): Found {} workflows", projectId, workflowInfos.size());
            }

            return workflowInfos;
        } catch (Exception e) {
            log.error("listWorkflows({}): Failed to list project workflows", projectId, e);

            throw new ExecutionException(
                "Failed to list project workflows: " + e.getMessage(), e, ProjectWorkflowErrorType.LIST_WORKFLOWS);
        }
    }

    @Tool(
        description = "Full-text search across the workflows of a workspace's projects. Returns a list of workflows matching the search query in name or description. An empty list means nothing matched; a project outside the workspace, or an invocation whose workspace cannot be resolved, is reported as an error instead.")
    public List<WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the project") Long projectId,
        @ToolParam(
            required = false, description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) Long workspaceId,
        ToolContext toolContext) {

        Long scopedProjectId = scopedProjectGuard.fetchScopedProjectId(toolContext);

        if (scopedProjectId != null) {
            if (projectId != null) {
                scopedProjectGuard.checkProject(projectId, toolContext, ProjectWorkflowErrorType.SEARCH_WORKFLOWS);
            }

            projectId = scopedProjectId;
            workspaceId = null;
        }

        Long resolvedWorkspaceId = null;

        if (scopedProjectId == null) {
            try {
                resolvedWorkspaceId = AutomationToolInvocationContext.resolveWorkspaceId(
                    workspaceId, toolContext, workspaceFacade);
            } catch (WorkspaceNotAccessibleException exception) {
                throw new ExecutionException(
                    exception.getMessage(), exception, ProjectWorkflowErrorType.SEARCH_WORKFLOWS);
            }
        }

        if (projectId == null && resolvedWorkspaceId == null) {
            throw new ExecutionException(
                "Either projectId or workspaceId is required - call listWorkspaces to find the workspace id.",
                ProjectWorkflowErrorType.SEARCH_WORKFLOWS);
        }

        try {
            List<ProjectWorkflowDTO> allWorkflows = projectId == null
                ? projectFacade.getWorkspaceProjectWorkflows(resolvedWorkspaceId)
                : projectWorkflowFacade.getProjectWorkflows(projectId);

            String lowerQuery = StringUtils.trim(query.toLowerCase());

            List<WorkflowInfo> matchingWorkflow = allWorkflows.stream()
                .filter(workflow -> {
                    String name = workflow.getLabel();

                    name = name != null ? name.toLowerCase() : "";

                    String description = workflow.getDescription();

                    description = description != null ? description.toLowerCase() : "";

                    return name.contains(lowerQuery) || description.contains(lowerQuery);
                })
                .map(workflow -> new WorkflowInfo(
                    workflow.getId(), workflow.getProjectWorkflowId(), workflow.getWorkflowUuid(), workflow.getLabel(),
                    workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                    workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                    workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null))
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "searchWorkflows({}, {}): Found {} workflows matching query '{}'", query, projectId,
                    matchingWorkflow.size(), query);
            }

            return matchingWorkflow;
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception e) {
            log.error("searchWorkflows({}, {}): Failed to search workflows with query '{}'", query, projectId, query,
                e);
            throw new ExecutionException(
                "Failed to search workflows: " + e.getMessage(), e, ProjectWorkflowErrorType.SEARCH_WORKFLOWS);
        }
    }

    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(
            description = "The definition for the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        ToolContext toolContext) {

        scopedProjectGuard.checkProject(projectId, toolContext, ProjectWorkflowErrorType.CREATE_WORKFLOW);
        checkAllowedComponents(definition, toolContext, ProjectWorkflowErrorType.CREATE_WORKFLOW);

        try {
            ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, definition);

            if (log.isDebugEnabled()) {
                log.debug("createProjectWorkflow({}): Created workflow for project {}", projectId, projectId);
            }

            capturePersistedWorkflow(
                toolContext, true, projectWorkflow.getWorkflowId(), projectWorkflow.getProjectId(),
                projectWorkflow.getId(), extractWorkflowName(definition));

            return new ProjectWorkflowInfo(
                projectWorkflow.getId(), projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion(),
                projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString(),
                projectWorkflow.getCreatedDate() != null ? projectWorkflow.getCreatedDate() : null,
                projectWorkflow.getLastModifiedDate() != null ? projectWorkflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            log.error(
                "createProjectWorkflow({}): Failed to create workflow for project {}", projectId, projectId, e);

            throw new ExecutionException(
                "Failed to create project workflow: " + e.getMessage(), e,
                ProjectWorkflowErrorType.CREATE_WORKFLOW);
        }
    }

    @Tool(description = "Delete a workflow. Returns a confirmation message.")
    public String deleteWorkflow(
        @ToolParam(description = "The ID of the workflow to delete") String workflowId, ToolContext toolContext) {

        scopedProjectGuard.checkWorkflow(workflowId, toolContext, ProjectWorkflowErrorType.DELETE_WORKFLOW);

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            String workflowName = projectWorkflowDTO.getLabel();

            projectWorkflowFacade.deleteWorkflow(projectWorkflowDTO.getId());

            if (log.isDebugEnabled()) {
                log.debug(
                    "deleteWorkflow({}): Deleted workflow {} with name '{}'", workflowId, workflowId, workflowName);
            }

            return "Workflow '" + workflowName + "' (ID: " + workflowId + ") has been successfully deleted.";
        } catch (Exception e) {
            log.error("deleteWorkflow({}): Failed to delete workflow {}", workflowId, workflowId, e);

            throw new ExecutionException(
                "Failed to delete workflow: " + e.getMessage(), e, ProjectWorkflowErrorType.DELETE_WORKFLOW);
        }
    }

    @Tool(
        description = "Update the workflow definition. Pass the version returned by the getWorkflow call the new definition is based on; if the workflow changed since then the update is rejected and the workflow must be re-read with getWorkflow. Returns the updated workflow id, name, definition and new version.")
    public WorkflowInfo updateWorkflow(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(
            description = "The new definition of the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        @ToolParam(
            description = "The version of the workflow, as returned by the getWorkflow call this definition is " +
                "based on") int version,
        ToolContext toolContext) {

        scopedProjectGuard.checkWorkflow(workflowId, toolContext, ProjectWorkflowErrorType.UPDATE_WORKFLOW);
        checkAllowedComponents(definition, toolContext, ProjectWorkflowErrorType.UPDATE_WORKFLOW);

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            if (projectWorkflowDTO.getVersion() != version) {
                throw new ExecutionException(
                    ("Workflow %s was modified after version %d was read (current version %d), so nothing was " +
                        "written. Call getWorkflow to re-read it, re-apply your changes to the current definition " +
                        "and retry updateWorkflow with the current version.")
                            .formatted(workflowId, version, projectWorkflowDTO.getVersion()),
                    ProjectWorkflowErrorType.UPDATE_WORKFLOW);
            }

            ProjectWorkflowDTO updatedProjectWorkflowDTO = projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), definition, version);

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateWorkflow({}): Updated workflow {} with name '{}'", workflowId,
                    updatedProjectWorkflowDTO.getId(), updatedProjectWorkflowDTO.getLabel());
            }

            captureUpdatedWorkflow(toolContext, workflowId, updatedProjectWorkflowDTO, definition);

            return new WorkflowInfo(
                updatedProjectWorkflowDTO.getId(), updatedProjectWorkflowDTO.getProjectWorkflowId(),
                updatedProjectWorkflowDTO.getWorkflowUuid(), updatedProjectWorkflowDTO.getLabel(),
                updatedProjectWorkflowDTO.getDescription(), definition, updatedProjectWorkflowDTO.getVersion(),
                updatedProjectWorkflowDTO.getCreatedDate(), updatedProjectWorkflowDTO.getLastModifiedDate());
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception e) {
            log.error("updateWorkflow({}): Failed to update workflow {}", workflowId, workflowId, e);

            throw new ExecutionException(
                "Failed to update workflow: " + e.getMessage(), e, ProjectWorkflowErrorType.UPDATE_WORKFLOW);
        }
    }

    @Tool(
        description = "Bind a connection the user picked to a workflow node so the workflow can be test-run. Call this "
            +
            "AFTER the workflow exists (createProjectWorkflow or updateWorkflow) AND after the user picks an existing "
            +
            "connection (selectConnection) or creates one (createConnection). NEVER put a connectionId or id inside " +
            "the workflow definition JSON: the definition's 'connections' block only declares the required component " +
            "(componentName/componentVersion). The chosen connection instance is stored separately, per environment, " +
            "by this tool. The connection is bound for the environment the user is currently working in. Returns a " +
            "confirmation message.")
    public String saveWorkflowTestConnection(
        @ToolParam(description = "The id of the workflow the node belongs to") String workflowId,
        @ToolParam(
            description = "The workflow node (task or trigger) name the connection is for, e.g. 'sendChannelMessage_1'") String workflowNodeName,
        @ToolParam(
            description = "The connection key declared in the node's 'connections' block — usually the component name, e.g. 'slack'") String connectionKey,
        @ToolParam(description = "The id of the connection the user picked") long connectionId,
        ToolContext toolContext) {

        scopedProjectGuard.checkWorkflow(workflowId, toolContext, ProjectWorkflowErrorType.SAVE_TEST_CONNECTION);

        try {
            Long environmentId = resolveEnvironmentId(toolContext);

            if (environmentId == null) {
                log.warn(
                    "saveWorkflowTestConnection({}, {}): no environment in the tool context", workflowId,
                    workflowNodeName);

                throw new ExecutionException(
                    ("Could not determine which environment to bind connection %d to for node '%s', so nothing was "
                        + "written. Tell the user the node was NOT connected and have them bind it from the workflow "
                        + "editor of the environment they are working in.")
                            .formatted(connectionId, workflowNodeName),
                    ProjectWorkflowErrorType.SAVE_TEST_CONNECTION);
            }

            checkConnectionAccessible(connectionId, getWorkflowWorkspaceId(workflowId), environmentId);

            projectWorkflowFacade.saveWorkflowTestConnection(
                workflowId, workflowNodeName, connectionKey, connectionId, environmentId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "saveWorkflowTestConnection({}, {}, {}, {}): Bound connection in environment {}", workflowId,
                    workflowNodeName, connectionKey, connectionId, environmentId);
            }

            return "Connection " + connectionId + " has been bound to node '" + workflowNodeName + "' for testing.";
        } catch (ExecutionException executionException) {
            throw executionException;
        } catch (Exception e) {
            log.error(
                "saveWorkflowTestConnection({}, {}, {}, {}): Failed to bind connection", workflowId, workflowNodeName,
                connectionKey, connectionId, e);

            throw new ExecutionException(
                ("Could not bind connection %d to node '%s' (key '%s'): %s. This node was NOT connected — tell the "
                    + "user and do not report it as done. If it is an AI model or other element inside an AI Agent, it "
                    + "must be connected from the workflow editor. Otherwise verify the connection exists in the "
                    + "current environment and that the connection key matches the node.")
                        .formatted(connectionId, workflowNodeName, connectionKey, e.getMessage()),
                e, ProjectWorkflowErrorType.SAVE_TEST_CONNECTION);
        }
    }

    private void checkAllowedComponents(
        String definition, @Nullable ToolContext toolContext,
        ProjectWorkflowErrorType projectWorkflowErrorType) {

        List<String> disallowedComponentNames = AllowedComponentNamesUtils.getDisallowedComponentNames(
            definition, toolContext, componentDefinitionService);

        if (!disallowedComponentNames.isEmpty()) {
            throw new ExecutionException(
                AllowedComponentNamesUtils.getNotAllowedMessage(disallowedComponentNames),
                projectWorkflowErrorType);
        }
    }

    private long getWorkflowWorkspaceId(String workflowId) {
        ProjectDTO projectDTO = projectFacade.getProject(projectWorkflowFacade.getWorkflowProjectId(workflowId));

        return Objects.requireNonNull(projectDTO.workspaceId());
    }

    private void checkConnectionAccessible(long connectionId, long workspaceId, long environmentId) {
        List<ConnectionDTO> connections = workspaceConnectionFacade.getConnections(
            workspaceId, null, null, environmentId, null);

        boolean found = connections.stream()
            .anyMatch(connection -> Objects.equals(connection.id(), connectionId));

        if (!found) {
            throw new IllegalArgumentException(
                "Connection " + connectionId + " not found in the workflow's workspace and current environment");
        }
    }

    private static @Nullable Long resolveEnvironmentId(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        Long environmentId = asLong(context.get(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY));

        if (environmentId == null) {
            environmentId = asLong(context.get(AGENT_TOOL_ENVIRONMENT_ID_KEY));
        }

        return environmentId;
    }

    private static @Nullable Long asLong(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException numberFormatException) {
                log.warn("Ignoring malformed environment id '{}' in the tool context", string);

                return null;
            }
        }

        return null;
    }

    private void captureUpdatedWorkflow(
        @Nullable ToolContext toolContext, String workflowId, ProjectWorkflowDTO projectWorkflowDTO,
        String definition) {

        try {
            long projectId = projectWorkflowFacade.getWorkflowProjectId(workflowId);
            String workflowName = extractWorkflowName(definition);

            capturePersistedWorkflow(
                toolContext, false, workflowId, projectId, projectWorkflowDTO.getProjectWorkflowId(), workflowName);
        } catch (RuntimeException exception) {
            log.warn("Failed to capture updated workflow (workflowId={})", workflowId, exception);
        }
    }

    private static void capturePersistedWorkflow(
        @Nullable ToolContext toolContext, boolean created, String workflowId, long projectId,
        @Nullable Long projectWorkflowId, String workflowName) {

        if (toolContext == null) {
            return;
        }

        Map<String, Object> context = toolContext.getContext();

        if (context == null) {
            return;
        }

        Object holder = context.get(PERSISTED_WORKFLOW_CAPTURE_KEY);

        if (!(holder instanceof List<?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> captures = (List<Map<String, Object>>) holder;

        Map<String, Object> entry = new HashMap<>();

        entry.put("created", created);
        entry.put("workflowId", workflowId);
        entry.put("projectId", projectId);
        entry.put("projectWorkflowId", projectWorkflowId);
        entry.put("name", workflowName);

        captures.add(entry);
    }

    private static String extractWorkflowName(String definition) {
        try {
            String label = JsonUtils.read(definition, "label", String.class);

            return label != null && !label.isBlank() ? label : DEFAULT_WORKFLOW_NAME;
        } catch (RuntimeException exception) {
            log.warn(
                "Could not read the label out of a workflow definition; it is named '{}'",
                DEFAULT_WORKFLOW_NAME, exception);

            return DEFAULT_WORKFLOW_NAME;
        }
    }
}

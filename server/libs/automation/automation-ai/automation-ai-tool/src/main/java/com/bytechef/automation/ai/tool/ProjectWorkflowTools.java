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

import static com.bytechef.automation.ai.tool.exception.ProjectWorkflowToolErrorType.GET_WORKFLOW;

import com.bytechef.automation.ai.tool.exception.ProjectWorkflowToolErrorType;
import com.bytechef.automation.ai.tool.model.ProjectWorkflowInfo;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
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

    private final ProjectService projectService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;
    private final ObjectProvider<WorkflowTestConfigurationFacade> workflowTestConfigurationFacadeProvider;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowTools(
        ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowService projectWorkflowService, UserService userService, WorkspaceFacade workspaceFacade,
        ObjectProvider<WorkflowTestConfigurationFacade> workflowTestConfigurationFacadeProvider) {

        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
        this.workflowTestConfigurationFacadeProvider = workflowTestConfigurationFacadeProvider;
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId) {

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
        @ToolParam(description = "The ID of the project") long projectId) {

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
                "Failed to list project workflows: " + e.getMessage(), e, ProjectWorkflowToolErrorType.LIST_WORKFLOWS);
        }
    }

    @Tool(
        description = "Full-text search across workflows in projects. Returns a list of workflows matching the search query in name or description. An empty list means nothing matched; a project you cannot reach, or an invocation with no authenticated user, is reported as an error instead.")
    public List<WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the project") Long projectId) {

        try {
            Optional<User> currentUser = userService.fetchCurrentUser();

            if (currentUser.isEmpty()) {
                log.warn("searchWorkflows(): no authenticated user in the invocation; the search was not run");

                throw new ExecutionException(
                    "No authenticated user in this tool invocation, so no project could be searched. This is not a "
                        + "statement that there are no workflows.",
                    ProjectWorkflowToolErrorType.SEARCH_WORKFLOWS);
            }

            Set<Long> accessibleProjectIds = getAccessibleProjectIds(currentUser.get());

            List<ProjectWorkflowDTO> allWorkflows;

            if (projectId != null) {
                if (!accessibleProjectIds.contains(projectId)) {
                    throw new ExecutionException(
                        "Project " + projectId + " is not among the projects available to you, so it was not "
                            + "searched. This is not a statement that the project has no workflows.",
                        ProjectWorkflowToolErrorType.SEARCH_WORKFLOWS);
                }

                allWorkflows = projectWorkflowFacade.getProjectWorkflows(projectId);
            } else {
                allWorkflows = accessibleProjectIds.stream()
                    .flatMap(accessibleProjectId -> projectWorkflowFacade.getProjectWorkflows(accessibleProjectId)
                        .stream())
                    .toList();
            }

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
                "Failed to search workflows: " + e.getMessage(), e, ProjectWorkflowToolErrorType.SEARCH_WORKFLOWS);
        }
    }

    private Set<Long> getAccessibleProjectIds(User user) {
        return workspaceFacade.getUserWorkspaces(user.getId())
            .stream()
            .map(Workspace::getId)
            .filter(Objects::nonNull)
            .flatMap(workspaceId -> projectService.getWorkspaceProjectIds(workspaceId)
                .stream())
            .collect(Collectors.toSet());
    }

    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(
            description = "The definition for the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        ToolContext toolContext) {

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
                ProjectWorkflowToolErrorType.CREATE_WORKFLOW);
        }
    }

    @Tool(description = "Delete a workflow. Returns a confirmation message.")
    public String deleteWorkflow(
        @ToolParam(description = "The ID of the workflow to delete") String workflowId) {

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
                "Failed to delete workflow: " + e.getMessage(), e, ProjectWorkflowToolErrorType.DELETE_WORKFLOW);
        }
    }

    @Tool(
        description = "Update the workflow definition. Returns the updated workflow id, name and definition.")
    public WorkflowInfo updateWorkflow(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(
            description = "The new definition of the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        ToolContext toolContext) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), definition, projectWorkflowDTO.getVersion());

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateWorkflow({}): Updated workflow {} with name '{}'", workflowId, projectWorkflowDTO.getId(),
                    projectWorkflowDTO.getLabel());
            }

            captureUpdatedWorkflow(toolContext, workflowId, projectWorkflowDTO, definition);

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), definition, projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate(), projectWorkflowDTO.getLastModifiedDate());
        } catch (Exception e) {
            log.error("updateWorkflow({}): Failed to update workflow {}", workflowId, workflowId, e);

            throw new ExecutionException(
                "Failed to update workflow: " + e.getMessage(), e, ProjectWorkflowToolErrorType.UPDATE_WORKFLOW);
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

        try {
            WorkflowTestConfigurationFacade workflowTestConfigurationFacade =
                workflowTestConfigurationFacadeProvider.getIfAvailable();

            if (workflowTestConfigurationFacade == null) {
                throw new ExecutionException(
                    "Saving a workflow test connection is not supported in this deployment",
                    ProjectWorkflowToolErrorType.SAVE_TEST_CONNECTION);
            }

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
                    ProjectWorkflowToolErrorType.SAVE_TEST_CONNECTION);
            }

            workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
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
                e, ProjectWorkflowToolErrorType.SAVE_TEST_CONNECTION);
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
                return null;
            }
        }

        return null;
    }

    private void captureUpdatedWorkflow(
        @Nullable ToolContext toolContext, String workflowId, ProjectWorkflowDTO projectWorkflowDTO,
        String definition) {

        try {
            long projectId = projectWorkflowService.getProjectWorkflow(projectWorkflowDTO.getProjectWorkflowId())
                .getProjectId();
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

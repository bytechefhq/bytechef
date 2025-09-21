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

package com.bytechef.ai.mcp.tool.automation;

import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ProjectWorkflowTools.class);

    private final ProjectFacade projectFacade;
    private final TaskTools taskTools;

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

    @SuppressFBWarnings("EI")
    public ProjectWorkflowTools(ProjectFacade projectFacade, TaskTools taskTools) {
        this.projectFacade = projectFacade;
        this.taskTools = taskTools;
    }

    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(
            description = "The definition for the workflow. Needs to be in JSON format similar to "
                + DEFAULT_DEFINITION) String definition) {

        try {
            ProjectWorkflow projectWorkflow = projectFacade.addWorkflow(projectId, definition);

            if (logger.isDebugEnabled()) {
                logger.debug("Created workflow {} for project {}", definition, projectId);
            }

            return new ProjectWorkflowInfo(
                projectWorkflow.getId(), projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion(),
                projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString(),
                projectWorkflow.getCreatedDate() != null ? projectWorkflow.getCreatedDate() : null,
                projectWorkflow.getLastModifiedDate() != null ? projectWorkflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error(
                "Failed to create workflow {} for project {}", definition, projectId, e);

            throw new RuntimeException("Failed to create project workflow: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "List all workflows in a project. Returns a list of workflows with their basic information including id, name and description")
    public List<WorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the project") long projectId) {

        try {
            List<ProjectWorkflowDTO> workflows = projectFacade.getProjectWorkflows(projectId);

            List<WorkflowInfo> workflowInfos = workflows.stream()
                .map(workflow -> new WorkflowInfo(workflow.getId(), workflow.getProjectWorkflowId(),
                    workflow.getWorkflowUuid(),
                    workflow.getLabel(), workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                    workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                    workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} projects", workflowInfos.size());
            }

            return workflowInfos;
        } catch (Exception e) {
            logger.error("Failed to list project workflows", e);

            throw new RuntimeException("Failed to list project workflows: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") long workflowId) {

        try {
            ProjectWorkflowDTO workflow = projectFacade.getProjectWorkflow(workflowId);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved workflow {}", workflow.getProjectWorkflowId());
            }

            return new WorkflowInfo(
                workflow.getId(), workflow.getProjectWorkflowId(), workflow.getWorkflowUuid(), workflow.getLabel(),
                workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to get workflow {}", workflowId, e);

            throw new RuntimeException("Failed to get workflow: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Full-text search across workflows in a project. Returns a list of workflows matching the search query in name or description.")
    public List<WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(description = "The ID of the project (optional)") Long projectId) {

        try {
            List<ProjectWorkflowDTO> allWorkflows =
                projectId != null ? projectFacade.getProjectWorkflows(projectId) : projectFacade.getProjectWorkflows();

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

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} workflows matching query '{}'", matchingWorkflow.size(), query);
            }

            return matchingWorkflow;
        } catch (Exception e) {
            logger.error("Failed to search workflows with query '{}'", query, e);
            throw new RuntimeException("Failed to search workflows: " + e.getMessage(), e);
        }
    }

    @Tool(description = "Delete a workflow. Returns a confirmation message.")
    public String deleteWorkflow(
        @ToolParam(description = "The ID of the workflow to delete") String workflowId) {
        try {
            ProjectWorkflowDTO workflow = projectFacade.getProjectWorkflow(workflowId);

            String workflowName = workflow.getLabel();

            projectFacade.deleteWorkflow(workflow.getId());

            if (logger.isDebugEnabled()) {
                logger.debug("Deleted workflow {} with name '{}'", workflowId, workflowName);
            }

            return "Workflow '" + workflowName + "' (ID: " + workflowId + ") has been successfully deleted.";
        } catch (Exception e) {
            logger.error("Failed to delete workflow {}", workflowId, e);

            throw new RuntimeException("Failed to delete workflow: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Update the workflow definition. Returns the updated workflow id, name and definition.")
    public WorkflowInfo updateWorkflow(
        @ToolParam(description = "The ID of the workflow to update") long workflowId,
        @ToolParam(
            description = "The new definition of the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition) {

        try {
            ProjectWorkflowDTO workflow = projectFacade.getProjectWorkflow(workflowId);

            projectFacade.updateWorkflow(workflow.getId(), definition, workflow.getVersion());

            if (logger.isDebugEnabled()) {
                logger.debug("Updated workflow {} with name '{}'", workflow.getId(), workflow.getLabel());
            }

            return new WorkflowInfo(
                workflow.getId(), workflow.getProjectWorkflowId(), workflow.getWorkflowUuid(), workflow.getLabel(),
                workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to update workflow {}", workflowId, e);

            throw new RuntimeException("Failed to update workflow: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Validate a workflow configuration by checking its structure, properties and outputs against the task definitions. Returns validation results with any errors found")
    public WorkflowValidationResult validateWorkflow(
        @ToolParam(description = "The JSON string of the workflow to validate") String workflow) {

        try {
            StringBuilder errors = new StringBuilder("[");
            StringBuilder warnings = new StringBuilder("[");

            WorkflowValidator.validateWorkflow(
                workflow, taskTools::getTaskProperties, taskTools::getTaskOutputProperty, new HashMap<>(),
                new HashMap<>(), errors, warnings);

            errors.append("]");

            String errorMessages = StringUtils.trim(errors.toString());

            boolean isValid = errorMessages.equals("[]");

            warnings.append("]");

            String warningMessages = StringUtils.trim(warnings.toString());

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Validated workflow. Valid: {}, Errors: {}, Warnings: {}", isValid, errorMessages, warningMessages);
            }

            return new WorkflowValidationResult(isValid, errorMessages, warningMessages);
        } catch (Exception e) {
            logger.error("Failed to validate workflow", e);

            throw new RuntimeException("Failed to validate workflow", e);
        }
    }

    /**
     * Project workflow information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectWorkflowInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project workflow") Long id,
        @JsonProperty("project_id") @JsonPropertyDescription("The ID of the project this workflow belongs to") long projectId,
        @JsonProperty("project_version") @JsonPropertyDescription("The version of the project") int projectVersion,
        @JsonProperty("workflow_id") @JsonPropertyDescription("The unique identifier of the workflow") String workflowId,
        @JsonProperty("workflow_uuid") @JsonPropertyDescription("The uuid of the workflow") String workflowUuid,
        @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") Instant createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") Instant lastModifiedDate) {
    }

    /**
     * Workflow information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record WorkflowInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the workflow") String id,
        @JsonProperty("project_workflow_id") @JsonPropertyDescription("The unique identifier of the project workflow") Long projectWorkflowId,
        @JsonProperty("workflow_uuid") @JsonPropertyDescription("The uuid of the workflow") String workflowUuid,
        @JsonProperty("name") @JsonPropertyDescription("The name of the workflow") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the workflow") String description,
        @JsonProperty("definition") @JsonPropertyDescription("The definition of the workflow") String definition,
        @JsonProperty("version") @JsonPropertyDescription("The version of the workflow") int version,
        @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") Instant createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") Instant lastModifiedDate) {
    }

    /**
     * Workflow validation result record for the response.
     */
    @SuppressFBWarnings("EI")
    public record WorkflowValidationResult(
        @JsonProperty("valid") @JsonPropertyDescription("Whether the workflow is valid") boolean valid,
        @JsonProperty("errors") @JsonPropertyDescription("Error details, which need to be fixed before the workflow can be valid") String errors,
        @JsonProperty("warnings") @JsonPropertyDescription("Warning details that give additional information") String warnings) {
    }
}

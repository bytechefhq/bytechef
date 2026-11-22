/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.ee.embedded.ai.tool.model.CreatedIntegrationWorkflowInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationWorkflowInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationWorkflowSaveInfo;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.IntegrationWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(IntegrationWorkflowTools.class);

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

    private final IntegrationWorkflowFacade integrationWorkflowFacade;
    private final ObjectProvider<WorkflowValidatorFacade> workflowValidatorFacadeProvider;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowTools(
        IntegrationWorkflowFacade integrationWorkflowFacade,
        ObjectProvider<WorkflowValidatorFacade> workflowValidatorFacadeProvider) {

        this.integrationWorkflowFacade = integrationWorkflowFacade;
        this.workflowValidatorFacadeProvider = workflowValidatorFacadeProvider;
    }

    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed information including workflow_id, integration_workflow_id, name, description, version (pass it to updateWorkflow), definition, created date, last modified date.")
    public IntegrationWorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId) {

        try {
            IntegrationWorkflowDTO integrationWorkflowDTO =
                integrationWorkflowFacade.getIntegrationWorkflow(workflowId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "getWorkflow({}): Retrieved workflow {}", workflowId,
                    integrationWorkflowDTO.getIntegrationWorkflowId());
            }

            return toIntegrationWorkflowInfo(integrationWorkflowDTO);
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "getWorkflow(" + workflowId + ")", "Failed to get workflow", exception,
                IntegrationWorkflowErrorType.GET_WORKFLOW);
        }
    }

    @Tool(
        description = "List all workflows in an integration. Returns a list of workflows with their basic information including workflow_id, integration_workflow_id, name and description")
    public List<IntegrationWorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the integration") long integrationId) {

        try {
            List<IntegrationWorkflowDTO> workflows = integrationWorkflowFacade.getIntegrationWorkflows(integrationId);

            List<IntegrationWorkflowInfo> workflowInfos = workflows.stream()
                .map(IntegrationWorkflowTools::toIntegrationWorkflowInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug("listWorkflows({}): Found {} workflows", integrationId, workflowInfos.size());
            }

            return workflowInfos;
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "listWorkflows(" + integrationId + ")", "Failed to list integration workflows", exception,
                IntegrationWorkflowErrorType.LIST_WORKFLOWS);
        }
    }

    @Tool(
        description = "Full-text search across integration workflows. Returns a list of workflows matching the search query in name or description.")
    public List<IntegrationWorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the integration") Long integrationId) {

        try {
            List<IntegrationWorkflowDTO> allWorkflows = integrationId != null
                ? integrationWorkflowFacade.getIntegrationWorkflows(integrationId)
                : integrationWorkflowFacade.getIntegrationWorkflows();

            String lowerQuery = StringUtils.trim(query.toLowerCase());

            List<IntegrationWorkflowInfo> matchingWorkflows = allWorkflows.stream()
                .filter(workflow -> {
                    String name = workflow.getLabel();

                    name = name != null ? name.toLowerCase() : "";

                    String description = workflow.getDescription();

                    description = description != null ? description.toLowerCase() : "";

                    return name.contains(lowerQuery) || description.contains(lowerQuery);
                })
                .map(IntegrationWorkflowTools::toIntegrationWorkflowInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "searchWorkflows({}, {}): Found {} workflows matching query '{}'", query, integrationId,
                    matchingWorkflows.size(), query);
            }

            return matchingWorkflows;
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "searchWorkflows(" + query + ", " + integrationId + ")", "Failed to search workflows", exception,
                IntegrationWorkflowErrorType.SEARCH_WORKFLOWS);
        }
    }

    @Tool(
        description = "Create a new workflow in a ByteChef integration. The definition is validated first: if it has errors, nothing is saved and the errors are returned, so fix them and call this again with the complete definition. Returns the created workflow information including workflow_id (the workflowId used by the other workflow tools), integration_workflow_id, integration_id and integration_version.")
    public CreatedIntegrationWorkflowInfo createIntegrationWorkflow(
        @ToolParam(description = "The ID of the integration to add the workflow to") long integrationId,
        @ToolParam(
            description = "The definition for the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition) {

        try {
            validateBeforeSave(definition, null, IntegrationWorkflowErrorType.CREATE_WORKFLOW);

            long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(integrationId, definition);

            IntegrationWorkflowDTO integrationWorkflowDTO =
                integrationWorkflowFacade.getIntegrationWorkflow(integrationWorkflowId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "createIntegrationWorkflow({}): Created workflow {} for integration {}", integrationId,
                    integrationWorkflowId, integrationId);
            }

            return new CreatedIntegrationWorkflowInfo(
                integrationWorkflowDTO.getId(), integrationWorkflowDTO.getIntegrationWorkflowId(), integrationId,
                integrationWorkflowDTO.getIntegrationVersion(), integrationWorkflowDTO.getWorkflowUuid(),
                integrationWorkflowDTO.getCreatedDate(),
                integrationWorkflowDTO.getLastModifiedDate());
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "createIntegrationWorkflow(" + integrationId + ")", "Failed to create integration workflow",
                exception, IntegrationWorkflowErrorType.CREATE_WORKFLOW);
        }
    }

    @Tool(description = "Delete a workflow. Returns a confirmation message.")
    public String deleteWorkflow(
        @ToolParam(description = "The ID of the workflow to delete") String workflowId) {

        try {
            IntegrationWorkflowDTO integrationWorkflowDTO =
                integrationWorkflowFacade.getIntegrationWorkflow(workflowId);

            String workflowName = integrationWorkflowDTO.getLabel();

            integrationWorkflowFacade.deleteWorkflow(workflowId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "deleteWorkflow({}): Deleted workflow {} with name '{}'", workflowId, workflowId, workflowName);
            }

            return "Workflow '" + workflowName + "' (ID: " + workflowId + ") has been successfully deleted.";
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "deleteWorkflow(" + workflowId + ")", "Failed to delete workflow", exception,
                IntegrationWorkflowErrorType.DELETE_WORKFLOW);
        }
    }

    @Tool(
        description = "Update the workflow definition. Pass the version returned by the getWorkflow or updateWorkflow call the new definition is based on; if the workflow changed since then the update is rejected and the workflow must be re-read with getWorkflow. The definition is validated first: if it has errors, nothing is saved and the errors are returned, so fix them and call this again with the same version. Returns the updated workflow id, name, new version and any validation warnings; the saved definition is the one you sent.")
    public IntegrationWorkflowSaveInfo updateWorkflow(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(
            description = "The new definition of the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition,
        @ToolParam(
            description = "The version of the workflow, as returned by the getWorkflow call this definition is " +
                "based on") int version) {

        try {
            IntegrationWorkflowDTO integrationWorkflowDTO =
                integrationWorkflowFacade.getIntegrationWorkflow(workflowId);

            if (integrationWorkflowDTO.getVersion() != version) {
                throw staleWorkflowVersionException(workflowId, version, null);
            }

            List<String> warnings = validateBeforeSave(
                definition, workflowId, IntegrationWorkflowErrorType.UPDATE_WORKFLOW);

            IntegrationWorkflowDTO updatedIntegrationWorkflowDTO;

            try {
                updatedIntegrationWorkflowDTO = integrationWorkflowFacade.updateWorkflow(
                    workflowId, definition, version);
            } catch (OptimisticLockingFailureException exception) {
                throw staleWorkflowVersionException(workflowId, version, exception);
            }

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateWorkflow({}): Updated workflow {} with name '{}'", workflowId, workflowId,
                    updatedIntegrationWorkflowDTO.getLabel());
            }

            return new IntegrationWorkflowSaveInfo(
                updatedIntegrationWorkflowDTO.getId(), updatedIntegrationWorkflowDTO.getIntegrationWorkflowId(),
                updatedIntegrationWorkflowDTO.getLabel(), updatedIntegrationWorkflowDTO.getVersion(), warnings);
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "updateWorkflow(" + workflowId + ", " + version + ")", "Failed to update workflow", exception,
                IntegrationWorkflowErrorType.UPDATE_WORKFLOW);
        }
    }

    private List<String> validateBeforeSave(
        String definition, @Nullable String workflowId, IntegrationWorkflowErrorType integrationWorkflowErrorType) {

        WorkflowValidatorFacade workflowValidatorFacade = workflowValidatorFacadeProvider.getIfAvailable();

        if (workflowValidatorFacade == null) {
            return List.of();
        }

        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                definition, workflowId,
                environment == null ? Environment.DEVELOPMENT.ordinal() : environment.ordinal());

        List<String> errors = workflowValidationResult.savingErrors();

        if (!errors.isEmpty()) {
            throw new ExecutionException(
                "Nothing was saved: the workflow definition has " + errors.size() + " validation error(s). Fix " +
                    "every one of them and save the complete definition again.\n- " + String.join("\n- ", errors),
                integrationWorkflowErrorType);
        }

        return workflowValidationResult.warnings();
    }

    private static ExecutionException staleWorkflowVersionException(
        String workflowId, int version, @Nullable Exception cause) {

        String message = ("Workflow %s was modified after version %d was read, so nothing was written. Call " +
            "getWorkflow to re-read it, re-apply your changes to the current definition and retry updateWorkflow " +
            "with the current version.").formatted(workflowId, version);

        if (cause == null) {
            return new ExecutionException(message, IntegrationWorkflowErrorType.UPDATE_WORKFLOW);
        }

        return new ExecutionException(message, cause, IntegrationWorkflowErrorType.UPDATE_WORKFLOW);
    }

    private static IntegrationWorkflowInfo toIntegrationWorkflowInfo(IntegrationWorkflowDTO integrationWorkflowDTO) {
        return new IntegrationWorkflowInfo(
            integrationWorkflowDTO.getId(), integrationWorkflowDTO.getIntegrationWorkflowId(),
            integrationWorkflowDTO.getWorkflowUuid(), integrationWorkflowDTO.getLabel(),
            integrationWorkflowDTO.getDescription(), integrationWorkflowDTO.getDefinition(),
            integrationWorkflowDTO.getVersion(), integrationWorkflowDTO.getCreatedDate(),
            integrationWorkflowDTO.getLastModifiedDate());
    }
}

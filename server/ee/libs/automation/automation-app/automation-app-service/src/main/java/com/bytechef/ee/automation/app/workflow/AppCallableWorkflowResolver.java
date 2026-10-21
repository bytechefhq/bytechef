/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
import com.bytechef.automation.configuration.workflow.WorkflowKind;
import com.bytechef.automation.configuration.workflow.WorkflowKindChecker;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves a workflow binding to the workflow it runs now. A binding names a project deployment and a workflow uuid;
 * the workflow id is looked up on every call, so a redeploy changes what the app runs. A binding resolves only while
 * the deployment exists, is enabled, belongs to the given workspace and environment, is not a system deployment (an API
 * collection, MCP server or A2A server deployment, or a deployment of a system project), its deployment workflow exists
 * and is enabled, the workflow is triggered by {@code workflow/newWorkflowCall} and that trigger's input schema is
 * readable JSON and the Response-to-workflow-call task's output schema, if any, is a string. This is the one definition
 * of a callable workflow: binding, listing, execution and {@link CallableWorkflowChecker#check(String)} all use it.
 * Deployments are looked up by workspace and environment, never through an app, so the app bindings and the agent
 * loop's listing share it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppCallableWorkflowResolver implements CallableWorkflowChecker, WorkflowKindChecker {

    static final String NO_CALL_WORKFLOW_TRIGGER =
        "no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall";
    static final String UNREADABLE_INPUT_SCHEMA = "unreadable inputSchema: ";
    static final String NON_STRING_INPUT_SCHEMA = "the inputSchema parameter must be a string holding a JSON Schema";
    static final String UNREADABLE_OUTPUT_SCHEMA = "unreadable outputSchema: ";
    static final String NON_STRING_OUTPUT_SCHEMA =
        "the outputSchema parameter must be a string holding a JSON Schema";

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectOwnershipService projectOwnershipService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AppCallableWorkflowResolver(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectOwnershipService projectOwnershipService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectOwnershipService = projectOwnershipService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowKind kind() {
        return WorkflowKind.APP_CALLABLE;
    }

    @Override
    public CallableWorkflowCheck check(String workflowId) {
        Optional<Workflow> workflow = workflowService.fetchWorkflow(workflowId);

        if (workflow.isEmpty()) {
            return new CallableWorkflowCheck(List.of("workflow %s does not exist".formatted(workflowId)), null);
        }

        Optional<WorkflowTrigger> callableTrigger = fetchCallableTrigger(workflow.get());

        if (callableTrigger.isEmpty()) {
            return new CallableWorkflowCheck(List.of(NO_CALL_WORKFLOW_TRIGGER), null);
        }

        WorkflowSchemas workflowSchemas = readSchemas(workflow.get(), callableTrigger.get());

        return new CallableWorkflowCheck(workflowSchemas.problems(), workflowSchemas.inputSchema());
    }

    public List<BindableWorkflow> getBindableWorkflows(long workspaceId, Environment environment) {
        List<BindableWorkflow> bindableWorkflows = new ArrayList<>();

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            false, environment, null, null, workspaceId);

        for (ProjectDeployment projectDeployment : projectDeployments) {
            if (!projectDeployment.isEnabled()) {
                continue;
            }

            Optional<Project> project = fetchProjectInWorkspace(workspaceId, projectDeployment);

            if (project.isEmpty()) {
                continue;
            }

            long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId());

            for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(projectDeploymentId)) {

                if (!projectDeploymentWorkflow.isEnabled()) {
                    continue;
                }

                toCallableWorkflow(projectDeploymentWorkflow).ifPresent(callableWorkflow -> bindableWorkflows.add(
                    new BindableWorkflow(
                        projectDeployment, project.get(),
                        projectWorkflowService.getProjectWorkflowUuid(
                            projectDeploymentId, projectDeploymentWorkflow.getWorkflowId()),
                        callableWorkflow)));
            }
        }

        return bindableWorkflows;
    }

    public Optional<CallableWorkflow> resolve(
        long workspaceId, Environment environment, long projectDeploymentId, String workflowUuid) {

        Optional<ProjectDeployment> fetchedProjectDeployment = projectDeploymentService.fetchProjectDeployment(
            projectDeploymentId);

        if (fetchedProjectDeployment.isEmpty()) {
            return Optional.empty();
        }

        ProjectDeployment projectDeployment = fetchedProjectDeployment.get();

        if (!projectDeployment.isEnabled() || projectDeployment.getEnvironment() != environment ||
            isFeatureOwned(projectDeployment)) {

            return Optional.empty();
        }

        Optional<Project> project = fetchProjectInWorkspace(workspaceId, projectDeployment);

        if (project.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> workflowId;

        try {
            workflowId = projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }

        return workflowId
            .flatMap(id -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, id))
            .filter(ProjectDeploymentWorkflow::isEnabled)
            .flatMap(this::toCallableWorkflow);
    }

    private Optional<Project> fetchProjectInWorkspace(long workspaceId, ProjectDeployment projectDeployment) {
        return projectService.fetchProject(projectDeployment.getProjectId())
            .filter(project -> Objects.equals(project.getWorkspaceId(), workspaceId));
    }

    private boolean isFeatureOwned(ProjectDeployment projectDeployment) {
        List<Long> projectDeploymentIds = List.of(Objects.requireNonNull(projectDeployment.getId()));

        for (ProjectDeploymentOwner projectDeploymentOwner : ProjectDeploymentOwner.values()) {
            Set<Long> ownedProjectDeploymentIds = projectOwnershipService.getOwnedProjectDeploymentIds(
                projectDeploymentIds, projectDeploymentOwner);

            if (!ownedProjectDeploymentIds.isEmpty()) {
                return true;
            }
        }

        Set<Long> ownedProjectIds = projectOwnershipService.getOwnedProjectIds(
            List.of(projectDeployment.getProjectId()), EnumSet.allOf(ProjectOwner.class));

        return !ownedProjectIds.isEmpty();
    }

    private Optional<CallableWorkflow> toCallableWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        String workflowId = projectDeploymentWorkflow.getWorkflowId();

        return workflowService.fetchWorkflow(workflowId)
            .flatMap(workflow -> fetchCallableTrigger(workflow).flatMap(trigger -> {
                WorkflowSchemas workflowSchemas = readSchemas(workflow, trigger);

                if (!workflowSchemas.readable()) {
                    return Optional.empty();
                }

                String label = workflow.getLabel() != null ? workflow.getLabel() : workflowId;

                return Optional.of(
                    new CallableWorkflow(
                        workflowId, trigger.getName(), label, workflow.getDescription(), workflowSchemas.inputSchema(),
                        workflowSchemas.outputSchema(), projectDeploymentWorkflow.getInputs()));
            }));
    }

    /**
     * The one readability rule of a callable workflow's schemas, shared by {@link #check(String)} and the binding,
     * listing and execution paths so they cannot disagree: the trigger's inputSchema and the Response-to-workflow-call
     * task's outputSchema must each be absent or a string, and the inputSchema string must be readable JSON Schema.
     */
    private static WorkflowSchemas readSchemas(Workflow workflow, WorkflowTrigger workflowTrigger) {
        List<String> problems = new ArrayList<>();

        String inputSchema = null;
        Map<String, ?> triggerParameters = workflowTrigger.getParameters();

        Object inputSchemaValue = triggerParameters.get(WorkflowConstants.INPUT_SCHEMA);

        if (inputSchemaValue != null && !(inputSchemaValue instanceof String)) {
            problems.add(UNREADABLE_INPUT_SCHEMA + NON_STRING_INPUT_SCHEMA);
        } else {
            inputSchema = blankToNull((String) inputSchemaValue);

            try {
                AppWorkflowSchemas.checkReadable(inputSchema);
            } catch (IllegalArgumentException illegalArgumentException) {
                problems.add(UNREADABLE_INPUT_SCHEMA + illegalArgumentException.getMessage());
            }
        }

        String outputSchema = null;
        Object outputSchemaValue = fetchOutputSchemaValue(workflow);

        if (outputSchemaValue instanceof String string) {
            outputSchema = blankToNull(string);
        } else if (outputSchemaValue != null) {
            problems.add(UNREADABLE_OUTPUT_SCHEMA + NON_STRING_OUTPUT_SCHEMA);
        }

        return new WorkflowSchemas(List.copyOf(problems), inputSchema, outputSchema);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Optional<WorkflowTrigger> fetchCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            if (isWorkflowOperation(workflowTrigger.getType(), WorkflowConstants.NEW_WORKFLOW_CALL)) {
                return Optional.of(workflowTrigger);
            }
        }

        return Optional.empty();
    }

    private static @Nullable Object fetchOutputSchemaValue(Workflow workflow) {
        for (WorkflowTask workflowTask : workflow.getTasks(true)) {
            if (isWorkflowOperation(workflowTask.getType(), WorkflowConstants.RESPONSE_TO_WORKFLOW_CALL)) {
                Map<String, ?> taskParameters = workflowTask.getParameters();

                return taskParameters.get(WorkflowConstants.OUTPUT_SCHEMA);
            }
        }

        return null;
    }

    private static boolean isWorkflowOperation(String type, String operation) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
            Objects.equals(workflowNodeType.operation(), operation);
    }

    /**
     * A project deployment workflow resolved to the workflow it runs, its Call-workflow trigger and its schemas.
     */
    @SuppressFBWarnings("EI")
    public record CallableWorkflow(
        String workflowId, String triggerName, String label, @Nullable String description,
        @Nullable String inputSchema, @Nullable String outputSchema, Map<String, ?> deploymentInputs) {
    }

    private record WorkflowSchemas(
        List<String> problems, @Nullable String inputSchema, @Nullable String outputSchema) {

        boolean readable() {
            return problems.isEmpty();
        }
    }

    @SuppressFBWarnings("EI")
    public record BindableWorkflow(
        ProjectDeployment projectDeployment, Project project, String workflowUuid,
        CallableWorkflow callableWorkflow) {
    }
}

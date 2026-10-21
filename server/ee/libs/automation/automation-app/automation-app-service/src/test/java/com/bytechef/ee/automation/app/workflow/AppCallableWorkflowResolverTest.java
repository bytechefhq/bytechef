/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
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
import com.bytechef.automation.configuration.workflow.WorkflowKind;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.BindableWorkflow;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.CallableWorkflow;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AppCallableWorkflowResolverTest {

    private static final long DEPLOYMENT_ID = 11L;
    private static final String INPUT_SCHEMA = "{\"type\": \"object\", \"required\": [\"orderId\"]}";
    private static final String OBJECT_VALUED_INPUT_SCHEMA_PARAMETERS =
        "{\"inputSchema\": {\"type\": \"object\", \"required\": [\"orderId\"]}}";
    private static final String OBJECT_VALUED_OUTPUT_SCHEMA_PARAMETERS =
        "{\"outputSchema\": {\"type\": \"object\"}}";
    private static final long PROJECT_ID = 3L;
    private static final String STRING_OUTPUT_SCHEMA_PARAMETERS =
        "{\"outputSchema\": \"{\\\"type\\\": \\\"object\\\"}\"}";
    private static final String WORKFLOW_UUID = "0b9f0b0e-5f1e-4d0a-8c52-2c3a0a8d8b11";
    private static final long WORKSPACE_ID = 7L;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectOwnershipService projectOwnershipService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    private AppCallableWorkflowResolver resolver;

    @BeforeEach
    void beforeEach() {
        resolver = new AppCallableWorkflowResolver(
            projectDeploymentService, projectDeploymentWorkflowService, projectOwnershipService, projectService,
            projectWorkflowService, workflowService);
    }

    @Test
    void testKindIsAppCallable() {
        assertThat(resolver.kind()).isEqualTo(WorkflowKind.APP_CALLABLE);
    }

    @Test
    void testResolvesTheCurrentWorkflowOfTheDeployment() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow("wf-2", true, workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall"));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));

        Optional<CallableWorkflow> callableWorkflow =
            resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID);

        assertThat(callableWorkflow).isPresent();

        CallableWorkflow resolved = callableWorkflow.get();

        assertThat(resolved.workflowId()).isEqualTo("wf-2");
        assertThat(resolved.triggerName()).isEqualTo("trigger_1");
        assertThat(resolved.label()).isEqualTo("Approve refund");
        assertThat(resolved.description()).isEqualTo("Asks a manager");
        assertThat(resolved.inputSchema()).isEqualTo(INPUT_SCHEMA);
        assertThat(resolved.outputSchema()).isEqualTo("{\"type\": \"object\"}");
        assertThat(resolved.deploymentInputs()).isEqualTo(Map.of("region", "eu"));
    }

    @Test
    void testWorkflowWithoutALabelUsesItsId() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow(
            "wf-2", true,
            new Workflow(
                "wf-2",
                """
                    {"triggers": [{"name": "trigger_1", "type": "workflow/v1/newWorkflowCall", "parameters": {}}], \
                     "tasks": []}\
                    """,
                Workflow.Format.JSON));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID))
            .map(CallableWorkflow::label)
            .contains("wf-2");
    }

    @Test
    void testDeploymentInAnotherWorkspaceIsUnresolvable() {
        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(8L), true);

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testDeploymentInAnotherEnvironmentIsUnresolvable() {
        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.STAGING, true), project(WORKSPACE_ID), true);

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testDisabledDeploymentIsUnresolvable() {
        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, false), project(WORKSPACE_ID), true);

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testDisabledDeploymentWorkflowIsUnresolvable() {
        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID), false);

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testMissingDeploymentIsUnresolvable() {
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ProjectDeploymentOwner.class)
    void testFeatureOwnedDeploymentIsUnresolvable(ProjectDeploymentOwner projectDeploymentOwner) {
        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID), true);

        lenient()
            .when(projectOwnershipService.getOwnedProjectDeploymentIds(List.of(DEPLOYMENT_ID), projectDeploymentOwner))
            .thenReturn(Set.of(DEPLOYMENT_ID));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testDeploymentOfAFeatureOwnedProjectIsUnresolvable() {
        stubCallablePath(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID), true);

        when(projectOwnershipService.getOwnedProjectIds(List.of(PROJECT_ID), EnumSet.allOf(ProjectOwner.class)))
            .thenReturn(Set.of(PROJECT_ID));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testWorkflowWithoutACallWorkflowTriggerIsUnresolvable() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow("wf-2", true, workflow("wf-2", null, "webhook/v1/autoRespondWithHTTP200"));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testWorkflowWithAnUnreadableInputSchemaIsUnresolvable() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow("wf-2", true, workflow("wf-2", "{not json", "workflow/v1/newWorkflowCall"));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testMalformedWorkflowUuidIsUnresolvable() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, "not-a-uuid"))
            .thenThrow(new IllegalArgumentException("Invalid UUID string: not-a-uuid"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, "not-a-uuid")).isEmpty();
    }

    @Test
    void testResolveReadsTheCurrentWorkflowIdOnEveryCall() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow("wf-2", true, workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall"));
        stubWorkflow("wf-3", true, workflow("wf-3", INPUT_SCHEMA, "workflow/v1/newWorkflowCall"));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"), Optional.of("wf-3"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID))
            .map(CallableWorkflow::workflowId)
            .contains("wf-2");
        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID))
            .map(CallableWorkflow::workflowId)
            .contains("wf-3");
    }

    @Test
    void testBindableWorkflowsListOnlyCallWorkflowWorkflowsOfEnabledDeployments() {
        ProjectDeployment enabledDeployment = deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true);
        ProjectDeployment disabledDeployment = deployment(12L, Environment.PRODUCTION, false);

        when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, WORKSPACE_ID))
            .thenReturn(List.of(enabledDeployment, disabledDeployment));
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project(WORKSPACE_ID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(DEPLOYMENT_ID))
            .thenReturn(
                List.of(deploymentWorkflow("wf-2", true, DEPLOYMENT_ID),
                    deploymentWorkflow("wf-9", true, DEPLOYMENT_ID)));
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
        when(workflowService.fetchWorkflow("wf-9"))
            .thenReturn(Optional.of(workflow("wf-9", null, "webhook/v1/autoRespondWithHTTP200")));
        when(projectWorkflowService.getProjectWorkflowUuid(DEPLOYMENT_ID, "wf-2")).thenReturn(WORKFLOW_UUID);

        lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(12L))
            .thenReturn(List.of(deploymentWorkflow("wf-5", true, 12L)));
        lenient().when(workflowService.fetchWorkflow("wf-5"))
            .thenReturn(Optional.of(workflow("wf-5", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
        lenient().when(projectWorkflowService.getProjectWorkflowUuid(12L, "wf-5"))
            .thenReturn("disabled-uuid");

        List<BindableWorkflow> bindableWorkflows = resolver.getBindableWorkflows(WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(bindableWorkflows).hasSize(1);

        BindableWorkflow bindableWorkflow = bindableWorkflows.getFirst();

        assertThat(bindableWorkflow.projectDeployment()).isSameAs(enabledDeployment);
        assertThat(bindableWorkflow.workflowUuid()).isEqualTo(WORKFLOW_UUID);
        assertThat(bindableWorkflow.callableWorkflow()
            .workflowId()).isEqualTo("wf-2");
    }

    @Test
    void testBindableWorkflowsSkipAnUnreadableInputSchema() {
        ProjectDeployment enabledDeployment = deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, WORKSPACE_ID))
            .thenReturn(List.of(enabledDeployment));
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project(WORKSPACE_ID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(DEPLOYMENT_ID))
            .thenReturn(
                List.of(deploymentWorkflow("wf-2", true, DEPLOYMENT_ID),
                    deploymentWorkflow("wf-7", true, DEPLOYMENT_ID)));
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
        when(workflowService.fetchWorkflow("wf-7"))
            .thenReturn(Optional.of(workflow("wf-7", "{not json", "workflow/v1/newWorkflowCall")));
        when(projectWorkflowService.getProjectWorkflowUuid(DEPLOYMENT_ID, "wf-2")).thenReturn(WORKFLOW_UUID);

        lenient().when(projectWorkflowService.getProjectWorkflowUuid(DEPLOYMENT_ID, "wf-7"))
            .thenReturn("unreadable-uuid");

        List<BindableWorkflow> bindableWorkflows = resolver.getBindableWorkflows(WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(bindableWorkflows).hasSize(1);
        assertThat(bindableWorkflows.getFirst()
            .callableWorkflow()
            .workflowId()).isEqualTo("wf-2");
    }

    @Test
    void testCheckAcceptsACallWorkflowWorkflowAndReturnsItsInputSchema() {
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));

        CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

        assertThat(callableWorkflowCheck.callable()).isTrue();
        assertThat(callableWorkflowCheck.problems()).isEmpty();
        assertThat(callableWorkflowCheck.inputSchema()).isEqualTo(INPUT_SCHEMA);
    }

    @Test
    void testCheckReportsAWorkflowWithoutACallWorkflowTrigger() {
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", null, "manual/v1/manual")));

        CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

        assertThat(callableWorkflowCheck.callable()).isFalse();
        assertThat(callableWorkflowCheck.problems())
            .containsExactly("no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall");
    }

    @Test
    void testCheckReportsAnUnreadableInputSchema() {
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", "{not json", "workflow/v1/newWorkflowCall")));

        CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

        assertThat(callableWorkflowCheck.callable()).isFalse();
        assertThat(callableWorkflowCheck.problems())
            .singleElement()
            .asString()
            .startsWith("unreadable inputSchema: ");
    }

    @Test
    void testCheckReportsAnObjectValuedInputSchemaAsUnreadable() {
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflowWithTriggerParameters(
                "wf-2", OBJECT_VALUED_INPUT_SCHEMA_PARAMETERS, "workflow/v1/newWorkflowCall")));

        CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

        assertThat(callableWorkflowCheck.callable()).isFalse();
        assertThat(callableWorkflowCheck.problems())
            .containsExactly(
                "unreadable inputSchema: the inputSchema parameter must be a string holding a JSON Schema");
        assertThat(callableWorkflowCheck.inputSchema()).isNull();
    }

    @Test
    void testWorkflowWithAnObjectValuedInputSchemaIsUnresolvable() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow(
            "wf-2", true,
            workflowWithTriggerParameters(
                "wf-2", OBJECT_VALUED_INPUT_SCHEMA_PARAMETERS, "workflow/v1/newWorkflowCall"));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testCheckReportsAnObjectValuedOutputSchemaAsUnreadable() {
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflowWithObjectValuedOutputSchema("wf-2")));

        CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

        assertThat(callableWorkflowCheck.callable()).isFalse();
        assertThat(callableWorkflowCheck.problems())
            .containsExactly(
                "unreadable outputSchema: the outputSchema parameter must be a string holding a JSON Schema");
        assertThat(callableWorkflowCheck.inputSchema()).isEqualTo(INPUT_SCHEMA);
    }

    @Test
    void testWorkflowWithAnObjectValuedOutputSchemaIsUnresolvable() {
        stubDeployment(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));
        stubWorkflow("wf-2", true, workflowWithObjectValuedOutputSchema("wf-2"));

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testBindableWorkflowsSkipAnObjectValuedOutputSchema() {
        ProjectDeployment enabledDeployment = deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, WORKSPACE_ID))
            .thenReturn(List.of(enabledDeployment));
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project(WORKSPACE_ID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(DEPLOYMENT_ID))
            .thenReturn(
                List.of(deploymentWorkflow("wf-2", true, DEPLOYMENT_ID),
                    deploymentWorkflow("wf-8", true, DEPLOYMENT_ID)));
        when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
        when(workflowService.fetchWorkflow("wf-8"))
            .thenReturn(Optional.of(workflowWithObjectValuedOutputSchema("wf-8")));
        when(projectWorkflowService.getProjectWorkflowUuid(DEPLOYMENT_ID, "wf-2")).thenReturn(WORKFLOW_UUID);

        lenient().when(projectWorkflowService.getProjectWorkflowUuid(DEPLOYMENT_ID, "wf-8"))
            .thenReturn("object-output-schema-uuid");

        assertThat(resolver.getBindableWorkflows(WORKSPACE_ID, Environment.PRODUCTION))
            .extracting(BindableWorkflow::workflowUuid)
            .containsExactly(WORKFLOW_UUID);
    }

    @Test
    void testCheckReportsAMissingWorkflow() {
        when(workflowService.fetchWorkflow("wf-9")).thenReturn(Optional.empty());

        assertThat(resolver.check("wf-9")
            .problems()).containsExactly("workflow wf-9 does not exist");
    }

    private ProjectDeployment deployment(long id, Environment environment, boolean enabled) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setEnabled(enabled);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setProjectId(PROJECT_ID);

        return projectDeployment;
    }

    private ProjectDeploymentWorkflow deploymentWorkflow(String workflowId, boolean enabled, long deploymentId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setInputs(Map.of("region", "eu"));
        projectDeploymentWorkflow.setProjectDeploymentId(deploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private Project project(long workspaceId) {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(workspaceId);

        return project;
    }

    private void stubCallablePath(
        ProjectDeployment projectDeployment, Project project, boolean deploymentWorkflowEnabled) {

        lenient().when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
        lenient().when(projectService.fetchProject(projectDeployment.getProjectId()))
            .thenReturn(Optional.of(project));
        lenient().when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of("wf-2"));
        lenient().when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(DEPLOYMENT_ID, "wf-2"))
            .thenReturn(Optional.of(deploymentWorkflow("wf-2", deploymentWorkflowEnabled, DEPLOYMENT_ID)));
        lenient().when(workflowService.fetchWorkflow("wf-2"))
            .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
    }

    private void stubDeployment(ProjectDeployment projectDeployment, Project project) {
        when(projectDeploymentService.fetchProjectDeployment(anyLong())).thenReturn(Optional.of(projectDeployment));
        when(projectService.fetchProject(projectDeployment.getProjectId())).thenReturn(Optional.of(project));
    }

    private void stubDeploymentWorkflow(String workflowId, boolean enabled) {
        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(DEPLOYMENT_ID, workflowId))
            .thenReturn(Optional.of(deploymentWorkflow(workflowId, enabled, DEPLOYMENT_ID)));
    }

    private void stubWorkflow(String workflowId, boolean enabled, Workflow workflow) {
        stubDeploymentWorkflow(workflowId, enabled);

        when(workflowService.fetchWorkflow(workflowId)).thenReturn(Optional.of(workflow));
    }

    private static Workflow workflow(String id, @Nullable String inputSchema, String triggerType) {
        String triggerParameters = inputSchema == null
            ? "{}" : "{\"inputSchema\": " + JsonUtils.write(inputSchema) + "}";

        return workflowWithTriggerParameters(id, triggerParameters, triggerType);
    }

    private static Workflow workflowWithObjectValuedOutputSchema(String id) {
        return workflow(
            id, "{\"inputSchema\": " + JsonUtils.write(INPUT_SCHEMA) + "}", "workflow/v1/newWorkflowCall",
            OBJECT_VALUED_OUTPUT_SCHEMA_PARAMETERS);
    }

    private static Workflow workflowWithTriggerParameters(String id, String triggerParameters, String triggerType) {
        return workflow(id, triggerParameters, triggerType, STRING_OUTPUT_SCHEMA_PARAMETERS);
    }

    private static Workflow workflow(
        String id, String triggerParameters, String triggerType, String responseParameters) {

        return new Workflow(
            id,
            """
                {"label": "Approve refund", "description": "Asks a manager", \
                 "triggers": [{"name": "trigger_1", "type": "%s", "parameters": %s}], \
                 "tasks": [{"name": "workflow_1", "type": "workflow/v1/responseToWorkflowCall", \
                            "parameters": %s}]}\
                """.formatted(triggerType, triggerParameters, responseParameters),
            Workflow.Format.JSON);
    }
}

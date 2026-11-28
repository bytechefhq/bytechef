/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DeploymentWorkflowChangesTest {

    private static final long PROJECT_ID = 7L;
    private static final String PROJECT_NAME = "Billing";
    private static final long TARGET_ID = 100L;
    private static final UUID ORDERS_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INVOICES_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REFUNDS_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final List<ProjectWorkflow> allProjectWorkflows = new ArrayList<>();
    private final Map<String, ProjectWorkflow> sourceProjectWorkflowsByWorkflowId = new HashMap<>();

    private final DeploymentWorkflowChanges deploymentWorkflowChanges = new DeploymentWorkflowChanges(
        projectDeploymentWorkflowService, projectWorkflowService, workflowService);

    @Test
    void testNewTargetYieldsOnlyCreates() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");
        stubWorkflow("invoices-1", "Invoices", "https://invoices.example");

        addSourceProjectWorkflow(1, "orders-1", ORDERS_UUID);
        addSourceProjectWorkflow(1, "invoices-1", INVOICES_UUID);

        List<PromotionChange> changes = deploymentWorkflowChanges.predict(
            null, List.of(projectDeploymentWorkflow("orders-1"), projectDeploymentWorkflow("invoices-1")),
            sourceProjectWorkflowsByWorkflowId, PROJECT_NAME);

        assertThat(changes).containsExactly(
            new PromotionChange(
                Kind.CREATE, "DEPLOYMENT_WORKFLOW", ORDERS_UUID.toString(), PROJECT_NAME, "Orders", false),
            new PromotionChange(
                Kind.CREATE, "DEPLOYMENT_WORKFLOW", INVOICES_UUID.toString(), PROJECT_NAME, "Invoices", false));
    }

    @Test
    void testUnchangedWorkflowYieldsNothing() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");

        addSourceProjectWorkflow(1, "orders-1", ORDERS_UUID);
        stubTargetProjectDeploymentWorkflows("orders-1");

        List<PromotionChange> changes = deploymentWorkflowChanges.predict(
            target(), List.of(projectDeploymentWorkflow("orders-1")), sourceProjectWorkflowsByWorkflowId,
            PROJECT_NAME);

        assertThat(changes).isEmpty();
    }

    @Test
    void testChangedDefinitionYieldsUpdate() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");
        stubWorkflow("orders-2", "Orders", "https://orders-v2.example");

        addTargetProjectWorkflow(1, "orders-1", ORDERS_UUID);
        addSourceProjectWorkflow(2, "orders-2", ORDERS_UUID);
        stubTargetProjectDeploymentWorkflows("orders-1");

        List<PromotionChange> changes = deploymentWorkflowChanges.predict(
            target(), List.of(projectDeploymentWorkflow("orders-2")), sourceProjectWorkflowsByWorkflowId,
            PROJECT_NAME);

        assertThat(changes).containsExactly(
            new PromotionChange(
                Kind.UPDATE, "DEPLOYMENT_WORKFLOW", ORDERS_UUID.toString(), PROJECT_NAME, "Orders", false));
    }

    @Test
    void testTargetWorkflowMissingFromSourceYieldsDestructiveDelete() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");
        stubWorkflow("invoices-1", "Invoices", "https://invoices.example");

        addSourceProjectWorkflow(1, "orders-1", ORDERS_UUID);
        addTargetProjectWorkflow(1, "invoices-1", INVOICES_UUID);
        stubTargetProjectDeploymentWorkflows("orders-1", "invoices-1");

        List<PromotionChange> changes = deploymentWorkflowChanges.predict(
            target(), List.of(projectDeploymentWorkflow("orders-1")), sourceProjectWorkflowsByWorkflowId,
            PROJECT_NAME);

        assertThat(changes).containsExactly(
            new PromotionChange(
                Kind.DELETE, "DEPLOYMENT_WORKFLOW", INVOICES_UUID.toString(), PROJECT_NAME, "Invoices", true));
    }

    @Test
    void testSkippedSourceWorkflowYieldsDelete() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");
        stubWorkflow("invoices-1", "Invoices", "https://invoices.example");

        addSourceProjectWorkflow(1, "orders-1", ORDERS_UUID);
        addTargetProjectWorkflow(1, "invoices-1", INVOICES_UUID);
        stubTargetProjectDeploymentWorkflows("orders-1", "invoices-1");

        List<PromotionChange> changes = deploymentWorkflowChanges.predict(
            target(), List.of(projectDeploymentWorkflow("orders-1"), projectDeploymentWorkflow("invoices-1")),
            sourceProjectWorkflowsByWorkflowId, PROJECT_NAME);

        assertThat(changes).containsExactly(
            new PromotionChange(
                Kind.DELETE, "DEPLOYMENT_WORKFLOW", INVOICES_UUID.toString(), PROJECT_NAME, "Invoices", true));
    }

    @Test
    void testSameLabelDifferentUuidsAreSeparateChanges() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");
        stubWorkflow("orders-copy-1", "Orders", "https://orders-copy.example");
        stubWorkflow("orders-old-1", "Orders", "https://orders-old.example");

        addSourceProjectWorkflow(1, "orders-1", ORDERS_UUID);
        addSourceProjectWorkflow(1, "orders-copy-1", REFUNDS_UUID);
        addTargetProjectWorkflow(1, "orders-old-1", INVOICES_UUID);
        stubTargetProjectDeploymentWorkflows("orders-1", "orders-old-1");

        List<PromotionChange> changes = deploymentWorkflowChanges.predict(
            target(), List.of(projectDeploymentWorkflow("orders-1"), projectDeploymentWorkflow("orders-copy-1")),
            sourceProjectWorkflowsByWorkflowId, PROJECT_NAME);

        assertThat(changes).containsExactly(
            new PromotionChange(
                Kind.CREATE, "DEPLOYMENT_WORKFLOW", REFUNDS_UUID.toString(), PROJECT_NAME, "Orders", false),
            new PromotionChange(
                Kind.DELETE, "DEPLOYMENT_WORKFLOW", INVOICES_UUID.toString(), PROJECT_NAME, "Orders", true));
    }

    @Test
    void testTargetWorkflowInNoProjectVersionIsRejected() {
        stubWorkflow("orders-1", "Orders", "https://orders.example");

        addSourceProjectWorkflow(1, "orders-1", ORDERS_UUID);
        stubTargetProjectDeploymentWorkflows("orders-1", "orphan-1");

        assertThatThrownBy(
            () -> deploymentWorkflowChanges.predict(
                target(), List.of(projectDeploymentWorkflow("orders-1")), sourceProjectWorkflowsByWorkflowId,
                PROJECT_NAME))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(WorkflowErrorType.WORKFLOW_NOT_FOUND.getErrorKey());
    }

    private void addSourceProjectWorkflow(int projectVersion, String workflowId, UUID uuid) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(PROJECT_ID, projectVersion, workflowId, uuid);

        allProjectWorkflows.add(projectWorkflow);
        sourceProjectWorkflowsByWorkflowId.put(workflowId, projectWorkflow);

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID)).thenReturn(allProjectWorkflows);
    }

    private void addTargetProjectWorkflow(int projectVersion, String workflowId, UUID uuid) {
        allProjectWorkflows.add(new ProjectWorkflow(PROJECT_ID, projectVersion, workflowId, uuid));

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID)).thenReturn(allProjectWorkflows);
    }

    private void stubTargetProjectDeploymentWorkflows(String... workflowIds) {
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = new ArrayList<>();

        for (String workflowId : workflowIds) {
            projectDeploymentWorkflows.add(projectDeploymentWorkflow(workflowId));
        }

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(TARGET_ID))
            .thenReturn(projectDeploymentWorkflows);
    }

    private void stubWorkflow(String workflowId, String label, String uri) {
        String definition = ("{\"label\": \"%s\", \"tasks\": [{\"name\": \"http_1\", " +
            "\"type\": \"httpClient/v1/get\", \"parameters\": {\"uri\": \"%s\"}}]}").formatted(label, uri);

        when(workflowService.getWorkflow(workflowId))
            .thenReturn(new Workflow(workflowId, definition, Workflow.Format.JSON));
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static ProjectDeployment target() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.STAGING);
        projectDeployment.setId(TARGET_ID);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(1);

        return projectDeployment;
    }
}

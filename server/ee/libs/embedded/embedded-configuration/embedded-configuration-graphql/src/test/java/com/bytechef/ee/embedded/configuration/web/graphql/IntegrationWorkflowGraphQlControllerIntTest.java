/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeSummary;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionChange;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionValueChange;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeKind;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeTarget;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    IntegrationWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = IntegrationWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
class IntegrationWorkflowGraphQlControllerIntTest {

    private static final String PERMISSION_EXPRESSION = "metadata['tier'] == 'pro'";
    private static final String WORKFLOW_DEFINITION = """
        {
            "label": "Workflow One",
            "tasks": [
                {"name": "gmail_1", "type": "gmail/v1/sendEmail"}
            ]
        }
        """;
    private static final String WORKFLOW_UUID = "1f2e3d4c-5b6a-4789-8a9b-0c1d2e3f4a5b";

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Test
    void testUpdateIntegrationWorkflowPermissionExpressionReturnsDTO() {
        when(integrationWorkflowFacade.getIntegrationWorkflow(42L)).thenReturn(createIntegrationWorkflowDTO());

        this.graphQlTester
            .document("""
                mutation($permissionExpression: String) {
                    updateIntegrationWorkflowPermissionExpression(
                        integrationWorkflowId: "42", permissionExpression: $permissionExpression) {
                        id
                        label
                        integrationWorkflowId
                        workflowUuid
                        workflowTaskComponentNames
                        workflowTriggerComponentNames
                    }
                }
                """)
            .variable("permissionExpression", PERMISSION_EXPRESSION)
            .execute()
            .path("updateIntegrationWorkflowPermissionExpression.id")
            .entity(String.class)
            .isEqualTo("workflow1")
            .path("updateIntegrationWorkflowPermissionExpression.label")
            .entity(String.class)
            .isEqualTo("Workflow One")
            .path("updateIntegrationWorkflowPermissionExpression.integrationWorkflowId")
            .entity(String.class)
            .isEqualTo("42")
            .path("updateIntegrationWorkflowPermissionExpression.workflowUuid")
            .entity(String.class)
            .isEqualTo(WORKFLOW_UUID)
            .path("updateIntegrationWorkflowPermissionExpression.workflowTaskComponentNames")
            .entityList(String.class)
            .containsExactly("gmail")
            .path("updateIntegrationWorkflowPermissionExpression.workflowTriggerComponentNames")
            .entityList(String.class)
            .containsExactly("manual");

        verify(integrationWorkflowFacade).updatePermissionExpression(42L, PERMISSION_EXPRESSION);
        verify(integrationWorkflowFacade).getIntegrationWorkflow(42L);
        verifyNoInteractions(integrationWorkflowService);
    }

    @Test
    void testUpdateIntegrationWorkflowPermissionExpressionResolvesPermissionExpressionThroughServiceRead() {
        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(42L);

        integrationWorkflow.setPermissionExpression(PERMISSION_EXPRESSION);

        when(integrationWorkflowFacade.getIntegrationWorkflow(42L)).thenReturn(createIntegrationWorkflowDTO());
        when(integrationWorkflowService.getIntegrationWorkflow(42L)).thenReturn(integrationWorkflow);

        this.graphQlTester
            .document("""
                mutation($permissionExpression: String) {
                    updateIntegrationWorkflowPermissionExpression(
                        integrationWorkflowId: "42", permissionExpression: $permissionExpression) {
                        integrationWorkflowId
                        permissionExpression
                    }
                }
                """)
            .variable("permissionExpression", PERMISSION_EXPRESSION)
            .execute()
            .path("updateIntegrationWorkflowPermissionExpression.integrationWorkflowId")
            .entity(String.class)
            .isEqualTo("42")
            .path("updateIntegrationWorkflowPermissionExpression.permissionExpression")
            .entity(String.class)
            .isEqualTo(PERMISSION_EXPRESSION);

        verify(integrationWorkflowFacade).updatePermissionExpression(42L, PERMISSION_EXPRESSION);
        verify(integrationWorkflowService).getIntegrationWorkflow(42L);
        verifyNoMoreInteractions(integrationWorkflowService);
    }

    @Test
    void testIntegrationVersionWorkflowChanges() {
        when(integrationWorkflowFacade.getIntegrationVersionWorkflowChanges(7L, 2))
            .thenReturn(new VersionWorkflowChanges(
                1, 2, List.of(new VersionWorkflowChangeSummary(WORKFLOW_UUID, "Workflow One",
                    WorkflowChangeType.MODIFIED, 3))));

        this.graphQlTester
            .document("""
                query {
                    integrationVersionWorkflowChanges(integrationId: "7", integrationVersion: 2) {
                        baseVersion
                        version
                        workflows {
                            workflowUuid
                            label
                            changeType
                            changeCount
                        }
                    }
                }
                """)
            .execute()
            .path("integrationVersionWorkflowChanges.baseVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("integrationVersionWorkflowChanges.version")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("integrationVersionWorkflowChanges.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo(WORKFLOW_UUID)
            .path("integrationVersionWorkflowChanges.workflows[0].changeType")
            .entity(String.class)
            .isEqualTo("MODIFIED")
            .path("integrationVersionWorkflowChanges.workflows[0].changeCount")
            .entity(Integer.class)
            .isEqualTo(3);

        verify(integrationWorkflowFacade).getIntegrationVersionWorkflowChanges(7L, 2);
    }

    @Test
    void testIntegrationVersionWorkflowChange() {
        when(integrationWorkflowFacade.getIntegrationVersionWorkflowChange(7L, 2, WORKFLOW_UUID))
            .thenReturn(new VersionWorkflowChange(
                WORKFLOW_UUID, "Workflow One", WorkflowChangeType.MODIFIED, 1, 2,
                List.of(new WorkflowDefinitionChange(
                    WorkflowNodeChangeKind.MODIFIED, WorkflowNodeChangeTarget.TASK, "gmail_1", "gmail/v1/sendEmail",
                    "Send", null, List.of(new WorkflowDefinitionValueChange("parameters.to", "a", "b")))),
                "{}", "{\"label\":\"x\"}"));

        this.graphQlTester
            .document("""
                query {
                    integrationVersionWorkflowChange(
                        integrationId: "7", integrationVersion: 2, workflowUuid: "%s") {
                        workflowUuid
                        label
                        changeType
                        baseVersion
                        version
                        oldDefinition
                        newDefinition
                        changes {
                            kind
                            target
                            name
                            valueChanges {
                                path
                                oldValue
                                newValue
                            }
                        }
                    }
                }
                """.formatted(WORKFLOW_UUID))
            .execute()
            .path("integrationVersionWorkflowChange.changeType")
            .entity(String.class)
            .isEqualTo("MODIFIED")
            .path("integrationVersionWorkflowChange.baseVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("integrationVersionWorkflowChange.newDefinition")
            .entity(String.class)
            .isEqualTo("{\"label\":\"x\"}")
            .path("integrationVersionWorkflowChange.changes[0].kind")
            .entity(String.class)
            .isEqualTo("MODIFIED")
            .path("integrationVersionWorkflowChange.changes[0].target")
            .entity(String.class)
            .isEqualTo("TASK")
            .path("integrationVersionWorkflowChange.changes[0].valueChanges[0].path")
            .entity(String.class)
            .isEqualTo("parameters.to");

        verify(integrationWorkflowFacade).getIntegrationVersionWorkflowChange(7L, 2, WORKFLOW_UUID);
    }

    private IntegrationWorkflowDTO createIntegrationWorkflowDTO() {
        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(42L);

        integrationWorkflow.setIntegrationVersion(1);
        integrationWorkflow.setUuid(WORKFLOW_UUID);

        Workflow workflow = new Workflow("workflow1", WORKFLOW_DEFINITION, Workflow.Format.JSON);

        return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
    }
}

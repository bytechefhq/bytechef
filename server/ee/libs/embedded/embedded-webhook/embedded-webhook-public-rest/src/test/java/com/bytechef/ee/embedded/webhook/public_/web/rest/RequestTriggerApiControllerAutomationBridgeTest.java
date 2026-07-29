/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class RequestTriggerApiControllerAutomationBridgeTest {

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @Mock
    private WorkflowService workflowService;

    @Test
    void testIntegrationWorkflowResolutionIsUnchangedWhenOneExists() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-1"), Mockito.any()))
            .thenReturn(Optional.of("integration-wf-1"));

        // Existing behavior must run through unmodified: connectedUserCodeWorkflowReferenceFacade must never be
        // consulted once an integration workflow is found.
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(2L);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);
        Mockito.when(integrationInstanceService.getIntegrationInstance(1L, "integration-wf-1", Environment.PRODUCTION))
            .thenReturn(integrationInstance);
        Mockito.when(workflowService.getWorkflow("integration-wf-1"))
            .thenReturn(new Workflow(
                "{\"label\":\"Integration Workflow\",\"triggers\":[{\"name\":\"trigger_1\",\"type\":\"request/v1\"}],"
                    + "\"tasks\":[]}",
                Workflow.Format.JSON));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflow("uuid-1", null);
        }

        Mockito.verifyNoInteractions(connectedUserCodeWorkflowReferenceFacade);
        Mockito.verifyNoInteractions(connectedUserProjectFacade);
    }

    @Test
    void testAutomationBridgeUnsupportedOperationExceptionReturnsNotFoundAndIsLoggedOnceAcrossTwoCalls() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-unsupported"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenThrow(new UnsupportedOperationException());

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ResponseEntity<Object> firstResponseEntity;
        ResponseEntity<Object> secondResponseEntity;

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            // The exception must not propagate out of executeWorkflow (it would surface as a 500 in a distributed
            // webhook-app that only carries remote-client stubs for the automation-bridge facades) and must resolve
            // to the same 404 an unknown workflowUuid returns.
            firstResponseEntity = controller.executeWorkflow("uuid-unsupported", null);
            secondResponseEntity = controller.executeWorkflow("uuid-unsupported", null);
        }

        Assertions.assertEquals(HttpStatus.NOT_FOUND, firstResponseEntity.getStatusCode());
        Assertions.assertNull(firstResponseEntity.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, secondResponseEntity.getStatusCode());
        Assertions.assertNull(secondResponseEntity.getBody());
    }

    @Test
    void testUnknownWorkflowFallsThroughToTheAutomationBridge() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-2"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(List.of());

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            ResponseEntity<Object> responseEntity = controller.executeWorkflow("uuid-2", null);

            Assertions.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        }
    }

    @Test
    void testPublishedCatalogWorkflowWithEnabledReferenceDispatchesWithAutomationWorkflowExecutionId() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-3"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(true, "uuid-3"));

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow reference = referenceFor(42L, true, false);

        Mockito.when(
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                Mockito.eq("ext-1"), Mockito.eq("uuid-3"), Mockito.any()))
            .thenReturn(reference);
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("uuid-3"))
            .thenReturn("catalog-wf-3");
        Mockito.when(workflowService.getWorkflow("catalog-wf-3"))
            .thenReturn(requestTriggerWorkflow("trigger_3"));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflow("uuid-3", null);
        }

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        // isWorkflowDisabled is the first call dispatch() makes, so capturing it here pins the exact
        // WorkflowExecutionId that would go on to drive execution.
        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(42L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("uuid-3", workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_3", workflowExecutionId.getTriggerName());

        // Reused the code-reference path, never the visual copy path.
        Mockito.verifyNoInteractions(connectedUserProjectFacade);
    }

    @Test
    void testDisabledAndDanglingReferencesReturnByteIdenticalNotFoundResponses() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-5"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-6"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(true, "uuid-5", "uuid-6"));

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);
        Mockito.when(
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                Mockito.eq("ext-1"), Mockito.eq("uuid-5"), Mockito.any()))
            .thenReturn(referenceFor(42L, false, false));
        Mockito.when(
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                Mockito.eq("ext-1"), Mockito.eq("uuid-6"), Mockito.any()))
            .thenReturn(referenceFor(42L, true, true));

        ResponseEntity<Object> disabledResponseEntity;
        ResponseEntity<Object> danglingResponseEntity;

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            disabledResponseEntity = controller.executeWorkflow("uuid-5", null);
            danglingResponseEntity = controller.executeWorkflow("uuid-6", null);
        }

        Assertions.assertEquals(HttpStatus.NOT_FOUND, disabledResponseEntity.getStatusCode());
        Assertions.assertNull(disabledResponseEntity.getBody());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, danglingResponseEntity.getStatusCode());
        Assertions.assertNull(danglingResponseEntity.getBody());

        // No existence leak: a disabled reference and a dangling reference must be indistinguishable to the caller.
        Assertions.assertEquals(disabledResponseEntity, danglingResponseEntity);
    }

    @Test
    void testMissingConnectionExceptionReturnsConflictWithComponentName() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-7"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(true, "uuid-7"));

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);
        Mockito.when(
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                Mockito.eq("ext-1"), Mockito.eq("uuid-7"), Mockito.any()))
            .thenThrow(new MissingConnectionException("slack"));

        ResponseEntity<Object> responseEntity;

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            responseEntity = controller.executeWorkflow("uuid-7", null);
        }

        Assertions.assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        Assertions.assertEquals(Map.of("missingConnectionComponentName", "slack"), responseEntity.getBody());
    }

    @Test
    void testEnabledNonDanglingReferenceWithNullProjectDeploymentIdReturnsNotFound() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-8"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(true, "uuid-8"));

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        // projectDeploymentId is null here on purpose: an enabled, non-dangling reference whose
        // project_deployment_id column is null must not NPE on the auto-unboxing WorkflowExecutionId.of() call.
        Mockito.when(
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                Mockito.eq("ext-1"), Mockito.eq("uuid-8"), Mockito.any()))
            .thenReturn(referenceFor(null, true, false));

        ResponseEntity<Object> responseEntity;

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            responseEntity = controller.executeWorkflow("uuid-8", null);
        }

        Assertions.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        Assertions.assertNull(responseEntity.getBody());
    }

    @Test
    void testCrossUserResolutionUsesOnlyTheCallersConnectedUserIdentity() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("uuid-9"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(true, "uuid-9"));

        ConnectedUser connectedUserA =
            new ConnectedUser(Map.of(), "userA@example.com", true, "ext-a", 10L, "User A", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.eq("login-a"), Mockito.any()))
            .thenReturn(connectedUserA);
        Mockito.when(
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                Mockito.eq("ext-a"), Mockito.eq("uuid-9"), Mockito.any()))
            .thenReturn(referenceFor(42L, true, false));
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("uuid-9"))
            .thenReturn("catalog-wf-9");
        Mockito.when(workflowService.getWorkflow("catalog-wf-9"))
            .thenReturn(requestTriggerWorkflow("trigger_9"));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("login-a"));

            controller.executeWorkflow("uuid-9", null);
        }

        // Resolution went through connected user A's external id...
        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .getOrCreateReference(Mockito.eq("ext-a"), Mockito.eq("uuid-9"), Mockito.any());

        // ...and never through any other connected user's identity, pinning the per-caller scoping contract.
        Mockito.verify(connectedUserCodeWorkflowReferenceFacade, Mockito.never())
            .getOrCreateReference(Mockito.eq("ext-b"), Mockito.anyString(), Mockito.any());
        Mockito.verify(connectedUserService, Mockito.never())
            .getConnectedUser(Mockito.eq("login-b"), Mockito.any());
    }

    @Test
    void testOwnCopyModeWorkflowUuidDispatchesDirectly() {
        RequestTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow copy = copyModeFor(200L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copy));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(6L, 2, "copy-wf-1", UUID.fromString(
            "00000000-0000-0000-0000-000000000201"));

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(
            Mockito.eq(projectWorkflow.getUuidAsString()), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(projectWorkflowService.getProjectWorkflow(200L))
            .thenReturn(projectWorkflow);
        Mockito.when(projectDeploymentService.getProjectDeploymentId(6L, Environment.PRODUCTION))
            .thenReturn(500L);
        Mockito.when(
            projectWorkflowService.fetchProjectWorkflowWorkflowId(500L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-1"));
        Mockito.when(workflowService.getWorkflow("copy-wf-1"))
            .thenReturn(requestTriggerWorkflow("trigger_copy"));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflow(projectWorkflow.getUuidAsString(), null);
        }

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(500L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals(projectWorkflow.getUuidAsString(), workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_copy", workflowExecutionId.getTriggerName());

        // The catalog and code-reference facades are never consulted once the caller's own copy uuid resolves.
        Mockito.verifyNoInteractions(automationWorkflowProjectFacade);
        Mockito.verifyNoInteractions(connectedUserProjectFacade);
    }

    @Test
    void testTemplateUuidWithNoExistingCopyProvisionsThenFires() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("template-uuid-1"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(false, "template-uuid-1"));

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow provisionedCopy = copyModeFor(300L, true, false);

        // Present in every read regardless of call ordering: it is not yet a match for "template-uuid-1" (its own
        // uuid, resolved below, differs) nor carries copiedFromWorkflowUuid == "template-uuid-1" (unset on this test
        // fixture), so the pre-provisioning lookups correctly find nothing until the post-provisioning lookup
        // resolves it by its own (newly-minted) uuid.
        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(provisionedCopy));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(7L, 2, "copy-wf-2", UUID.fromString(
            "00000000-0000-0000-0000-000000000301"));

        Mockito.when(projectWorkflowService.getProjectWorkflow(300L))
            .thenReturn(projectWorkflow);
        Mockito.when(
            connectedUserProjectFacade.copyWorkflowTemplate(
                Mockito.eq("ext-1"), Mockito.eq("template-uuid-1"), Mockito.any()))
            .thenReturn(projectWorkflow.getUuidAsString());
        Mockito.when(projectDeploymentService.getProjectDeploymentId(7L, Environment.PRODUCTION))
            .thenReturn(600L);
        Mockito.when(
            projectWorkflowService.fetchProjectWorkflowWorkflowId(600L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-2"));
        Mockito.when(workflowService.getWorkflow("copy-wf-2"))
            .thenReturn(requestTriggerWorkflow("trigger_provisioned"));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflow("template-uuid-1", null);
        }

        Mockito.verify(connectedUserProjectFacade)
            .copyWorkflowTemplate(Mockito.eq("ext-1"), Mockito.eq("template-uuid-1"), Mockito.any());

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        Assertions.assertEquals(600L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("trigger_provisioned", workflowExecutionId.getTriggerName());
    }

    @Test
    void testRepeatTemplateUuidCallReusesTheExistingCopyWithoutProvisioningAgain() {
        RequestTriggerApiController controller = controller();

        Mockito.when(integrationWorkflowService.fetchLastWorkflowId(Mockito.eq("template-uuid-2"), Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(publishedProjectsFor(false, "template-uuid-2"));

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow existingCopy = copyModeFor(301L, true, false);

        existingCopy.setCopiedFromWorkflowUuid("template-uuid-2");

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(existingCopy));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(8L, 2, "copy-wf-3", UUID.fromString(
            "00000000-0000-0000-0000-000000000302"));

        Mockito.when(projectWorkflowService.getProjectWorkflow(301L))
            .thenReturn(projectWorkflow);
        Mockito.when(projectDeploymentService.getProjectDeploymentId(8L, Environment.PRODUCTION))
            .thenReturn(601L);
        Mockito.when(
            projectWorkflowService.fetchProjectWorkflowWorkflowId(601L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-3"));
        Mockito.when(workflowService.getWorkflow("copy-wf-3"))
            .thenReturn(requestTriggerWorkflow("trigger_existing"));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflow("template-uuid-2", null);
            controller.executeWorkflow("template-uuid-2", null);
        }

        // No duplicate: an existing copy is reused on every call, so the copy endpoint's facade method is never
        // invoked -- the design's "repeat call with the template uuid resolves the existing copy" contract.
        Mockito.verifyNoInteractions(connectedUserProjectFacade);

        Mockito.verify(webhookWorkflowExecutor, Mockito.times(2))
            .isWorkflowDisabled(Mockito.any());
    }

    private static Workflow requestTriggerWorkflow(String triggerName) {
        return new Workflow(
            "{\"label\":\"Catalog Workflow\",\"triggers\":[{\"name\":\"" + triggerName + "\",\"type\":\"request/v1\"}],"
                + "\"tasks\":[]}",
            Workflow.Format.JSON);
    }

    private static List<AutomationWorkflowProjectDTO> publishedProjectsFor(
        boolean codeWorkflowProject, String... workflowUuids) {

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates = Arrays.stream(workflowUuids)
            .map(
                workflowUuid -> new ConnectedUserWorkflowTemplateDTO(
                    workflowUuid, "Label", "Description", null, List.of(), List.of(), null))
            .toList();

        AutomationWorkflowProjectDTO project = new AutomationWorkflowProjectDTO(
            1L, "Project", "Description", null, List.of(), true, 1, 1, workflowTemplates, null,
            codeWorkflowProject);

        return List.of(project);
    }

    private static ConnectedUserProjectWorkflow referenceFor(
        Long projectDeploymentId, boolean enabled, boolean dangling) {

        return new ConnectedUserProjectWorkflow(
            1L, 5L, null, 1, "catalog-uuid", projectDeploymentId, enabled, dangling, null, 0);
    }

    private static ConnectedUserProjectWorkflow copyModeFor(
        Long projectWorkflowId, boolean enabled, boolean dangling) {

        return new ConnectedUserProjectWorkflow(
            2L, 5L, projectWorkflowId, 1, null, null, enabled, dangling, null, 0);
    }

    private RequestTriggerApiController controller() {
        // EnvironmentService.getEnvironment(String) is a default method; delegate to the real implementation so a
        // null xEnvironment resolves to Environment.PRODUCTION as it does in production, instead of Mockito's
        // ordinary null default answer.
        EnvironmentService environmentService = mock(EnvironmentService.class, Mockito.CALLS_REAL_METHODS);

        // Bare-minimum stubs so a request that does reach doProcessTrigger (the integration-workflow test) does not
        // NPE while building the WebhookRequest. Lenient because the automation-bridge test never reaches dispatch.
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

        Mockito.lenient()
            .when(httpServletRequest.getHeaderNames())
            .thenReturn(Collections.enumeration(List.of()));
        Mockito.lenient()
            .when(httpServletRequest.getParameterMap())
            .thenReturn(Map.of());
        Mockito.lenient()
            .when(httpServletRequest.getMethod())
            .thenReturn("GET");

        Mockito.lenient()
            .when(webhookWorkflowExecutor.getWebhookTriggerFlags(Mockito.any()))
            .thenReturn(new WebhookTriggerFlags(false, false, false, false));

        return new RequestTriggerApiController(
            new ApplicationProperties(), automationWorkflowProjectFacade, connectedUserCodeWorkflowReferenceFacade,
            connectedUserProjectFacade, connectedUserService, environmentService, mock(FileEntryTokens.class),
            httpServletRequest, mock(HttpServletResponse.class), projectDeploymentService, mock(TempFileStorage.class),
            webhookWorkflowExecutor, integrationInstanceService, integrationWorkflowService, projectWorkflowService,
            workflowService);
    }
}

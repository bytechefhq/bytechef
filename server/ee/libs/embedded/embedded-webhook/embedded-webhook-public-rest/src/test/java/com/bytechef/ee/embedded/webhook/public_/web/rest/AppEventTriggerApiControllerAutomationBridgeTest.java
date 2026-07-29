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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
class AppEventTriggerApiControllerAutomationBridgeTest {

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    @Mock
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

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
    void testEnabledReferenceWithAppEventTriggerIsIncludedInFanOut() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow reference = referenceFor("catalog-uuid-1", 77L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(reference));
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-1"))
            .thenReturn("catalog-wf-1");
        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(appEventWorkflow("t1"));
        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(captor.capture());

        WorkflowExecutionId workflowExecutionId = captor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(77L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("catalog-uuid-1", workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("t1", workflowExecutionId.getTriggerName());
    }

    @Test
    void testAutomationBridgeUnsupportedOperationExceptionSkipsTheBridgeSourceWithoutAffectingIntegrationFanOut() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);
        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenThrow(new UnsupportedOperationException());

        // Existing integration-instance fan-out, set up exactly as it was before this task, to prove it is
        // unaffected when the automation-bridge facade is a remote-client stub.
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(5L);

        Mockito.when(integrationInstanceService.getConnectedUserIntegrationInstances(1L, true))
            .thenReturn(List.of(integrationInstance));

        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(30L);

        Mockito.when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(5L))
            .thenReturn(List.of(integrationInstanceWorkflow));

        IntegrationInstanceConfigurationWorkflow configurationWorkflow = new IntegrationInstanceConfigurationWorkflow();

        configurationWorkflow.setWorkflowId("int-wf-1");

        Mockito.when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(30L))
            .thenReturn(configurationWorkflow);

        Mockito.when(workflowService.getWorkflow("int-wf-1"))
            .thenReturn(appEventWorkflow("t_int"));

        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(5L, 1, "int-wf-1", UUID.randomUUID());

        Mockito.when(integrationWorkflowService.getWorkflowIntegrationWorkflow("int-wf-1"))
            .thenReturn(integrationWorkflow);

        ResponseEntity<Void> firstResponseEntity;
        ResponseEntity<Void> secondResponseEntity;

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            // Called twice: the exception must never propagate and must not break a subsequent call either.
            firstResponseEntity = controller.executeWorkflows(null);
            secondResponseEntity = controller.executeWorkflows(null);
        }

        Assertions.assertEquals(HttpStatus.OK, firstResponseEntity.getStatusCode());
        Assertions.assertEquals(HttpStatus.OK, secondResponseEntity.getStatusCode());

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        // Only the pre-existing integration-instance loop fired, twice over (once per call) -- the automation-bridge
        // loop contributed nothing once its facade started throwing.
        Mockito.verify(webhookWorkflowExecutor, Mockito.times(2))
            .getWebhookTriggerFlags(captor.capture());

        Assertions.assertTrue(
            captor.getAllValues()
                .stream()
                .allMatch(
                    workflowExecutionId -> workflowExecutionId.getType() == PlatformType.EMBEDDED &&
                        workflowExecutionId.getJobPrincipalId() == 5L));
    }

    @Test
    void testDanglingReferenceIsSkipped() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow dangling = referenceFor("catalog-uuid-2", 78L, true, true);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(dangling));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        Mockito.verifyNoInteractions(webhookWorkflowExecutor);
        Mockito.verifyNoInteractions(projectWorkflowService);
    }

    @Test
    void testDisabledReferenceIsSkipped() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow disabled = referenceFor("catalog-uuid-3", 79L, false, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(disabled));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        Mockito.verifyNoInteractions(webhookWorkflowExecutor);
        Mockito.verifyNoInteractions(projectWorkflowService);
    }

    @Test
    void testReferenceWithoutAppEventTriggerIsSkipped() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow reference = referenceFor("catalog-uuid-4", 80L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(reference));
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-4"))
            .thenReturn("catalog-wf-4");
        Mockito.when(workflowService.getWorkflow("catalog-wf-4"))
            .thenReturn(
                new Workflow(
                    "{\"label\":\"No App Event\",\"triggers\":[{\"name\":\"t\",\"type\":\"request/v1\"}],"
                        + "\"tasks\":[]}",
                    Workflow.Format.JSON));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        Mockito.verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    void testFirstReferenceFailureDoesNotStopFanOutToRemainingReferences() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow failingReference = referenceFor("catalog-uuid-fail", 90L, true, false);
        ConnectedUserProjectWorkflow healthyReference = referenceFor("catalog-uuid-ok", 91L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(failingReference, healthyReference));

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-fail"))
            .thenThrow(new IllegalStateException("catalog workflow lookup failed"));
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-ok"))
            .thenReturn("catalog-wf-ok");
        Mockito.when(workflowService.getWorkflow("catalog-wf-ok"))
            .thenReturn(appEventWorkflow("t_ok"));
        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        ResponseEntity<Void> responseEntity;

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            responseEntity = controller.executeWorkflows(null);
        }

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        // Only the healthy reference reaches isWorkflowDisabled: the failing reference's exception is contained and
        // never propagates to (or stops the fan-out for) the reference that follows it.
        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(captor.capture());

        Assertions.assertEquals(91L, captor.getValue()
            .getJobPrincipalId());
    }

    @Test
    void testCrossUserResolutionUsesOnlyTheCallersConnectedUserId() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUserA =
            new ConnectedUser(Map.of(), "user-a@example.com", true, "ext-a", 10L, "User A", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.eq("login-a"), Mockito.any()))
            .thenReturn(connectedUserA);

        ConnectedUserProjectWorkflow referenceA = referenceFor("catalog-uuid-a", 77L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(10L))
            .thenReturn(List.of(referenceA));
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-a"))
            .thenReturn("catalog-wf-a");
        Mockito.when(workflowService.getWorkflow("catalog-wf-a"))
            .thenReturn(appEventWorkflow("t_a"));
        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("login-a"));

            controller.executeWorkflows(null);
        }

        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .getConnectedUserWorkflows(10L);

        // No cross-user leak: another connected user's id and login are never consulted while resolving this
        // request's fan-out.
        Mockito.verify(connectedUserCodeWorkflowReferenceFacade, Mockito.never())
            .getConnectedUserWorkflows(20L);
        Mockito.verify(connectedUserService, Mockito.never())
            .getConnectedUser(Mockito.eq("login-b"), Mockito.any());
    }

    @Test
    void testExistingIntegrationFanOutIsUnaffectedByAutomationBridgeReferences() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        // Existing integration-instance fan-out, set up exactly as it was before this task.
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(5L);

        Mockito.when(integrationInstanceService.getConnectedUserIntegrationInstances(1L, true))
            .thenReturn(List.of(integrationInstance));

        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(30L);

        Mockito.when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(5L))
            .thenReturn(List.of(integrationInstanceWorkflow));

        IntegrationInstanceConfigurationWorkflow configurationWorkflow = new IntegrationInstanceConfigurationWorkflow();

        configurationWorkflow.setWorkflowId("int-wf-1");

        Mockito.when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(30L))
            .thenReturn(configurationWorkflow);

        Mockito.when(workflowService.getWorkflow("int-wf-1"))
            .thenReturn(appEventWorkflow("t_int"));

        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(5L, 1, "int-wf-1", UUID.randomUUID());

        Mockito.when(integrationWorkflowService.getWorkflowIntegrationWorkflow("int-wf-1"))
            .thenReturn(integrationWorkflow);

        // A reference is present at the same time, to prove the new automation-bridge loop runs alongside the
        // integration loop rather than displacing it.
        ConnectedUserProjectWorkflow reference = referenceFor("catalog-uuid-1", 77L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(reference));
        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-1"))
            .thenReturn("catalog-wf-1");
        Mockito.when(workflowService.getWorkflow("catalog-wf-1"))
            .thenReturn(appEventWorkflow("t_auto"));
        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        ArgumentCaptor<WorkflowExecutionId> flagsCaptor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        Mockito.verify(webhookWorkflowExecutor, Mockito.times(2))
            .getWebhookTriggerFlags(flagsCaptor.capture());

        List<WorkflowExecutionId> capturedIds = flagsCaptor.getAllValues();

        // Old integration-instance loop: byte-for-byte unchanged, still keyed by PlatformType.EMBEDDED and the
        // integration instance / integration workflow uuid.
        Assertions.assertTrue(
            capturedIds.stream()
                .anyMatch(
                    workflowExecutionId -> workflowExecutionId.getType() == PlatformType.EMBEDDED &&
                        workflowExecutionId.getJobPrincipalId() == 5L &&
                        Objects.equals(workflowExecutionId.getWorkflowUuid(), integrationWorkflow.getUuidAsString()) &&
                        workflowExecutionId.getTriggerName()
                            .equals("t_int")));

        // New automation-bridge loop: added alongside it, keyed by PlatformType.AUTOMATION and the reference's
        // project deployment id.
        Assertions.assertTrue(
            capturedIds.stream()
                .anyMatch(
                    workflowExecutionId -> workflowExecutionId.getType() == PlatformType.AUTOMATION &&
                        workflowExecutionId.getJobPrincipalId() == 77L &&
                        workflowExecutionId.getTriggerName()
                            .equals("t_auto")));
    }

    @Test
    void testCopyModeWorkflowWithAppEventTriggerFires() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow copy = copyModeFor(200L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copy));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(6L, 2, "copy-wf-1", UUID.fromString(
            "00000000-0000-0000-0000-000000000201"));

        Mockito.when(projectWorkflowService.getProjectWorkflow(200L))
            .thenReturn(projectWorkflow);
        Mockito.when(projectDeploymentService.getProjectDeploymentId(6L, Environment.PRODUCTION))
            .thenReturn(500L);
        Mockito.when(
            projectWorkflowService.fetchProjectWorkflowWorkflowId(500L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-1"));
        Mockito.when(workflowService.getWorkflow("copy-wf-1"))
            .thenReturn(appEventWorkflow("t_copy"));
        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        Mockito.verify(webhookWorkflowExecutor)
            .isWorkflowDisabled(captor.capture());

        WorkflowExecutionId workflowExecutionId = captor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(500L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals(projectWorkflow.getUuidAsString(), workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("t_copy", workflowExecutionId.getTriggerName());
    }

    @Test
    void testMixedCopyAndReferenceWorkflowsBothFire() {
        AppEventTriggerApiController controller = controller();

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user-1@example.com", true, "ext-1", 1L, "User 1", 0);

        Mockito.when(connectedUserService.getConnectedUser(Mockito.anyString(), Mockito.any()))
            .thenReturn(connectedUser);

        ConnectedUserProjectWorkflow copy = copyModeFor(201L, true, false);
        ConnectedUserProjectWorkflow reference = referenceFor("catalog-uuid-mixed", 88L, true, false);

        Mockito.when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copy, reference));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(7L, 2, "copy-wf-2", UUID.fromString(
            "00000000-0000-0000-0000-000000000202"));

        Mockito.when(projectWorkflowService.getProjectWorkflow(201L))
            .thenReturn(projectWorkflow);
        Mockito.when(projectDeploymentService.getProjectDeploymentId(7L, Environment.PRODUCTION))
            .thenReturn(501L);
        Mockito.when(
            projectWorkflowService.fetchProjectWorkflowWorkflowId(501L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-2"));
        Mockito.when(workflowService.getWorkflow("copy-wf-2"))
            .thenReturn(appEventWorkflow("t_copy_mixed"));

        Mockito.when(projectWorkflowService.getLastPublishedWorkflowId("catalog-uuid-mixed"))
            .thenReturn("catalog-wf-mixed");
        Mockito.when(workflowService.getWorkflow("catalog-wf-mixed"))
            .thenReturn(appEventWorkflow("t_reference_mixed"));

        Mockito.when(webhookWorkflowExecutor.isWorkflowDisabled(Mockito.any()))
            .thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("user-1"));

            controller.executeWorkflows(null);
        }

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        Mockito.verify(webhookWorkflowExecutor, Mockito.times(2))
            .isWorkflowDisabled(captor.capture());

        List<WorkflowExecutionId> capturedIds = captor.getAllValues();

        Assertions.assertTrue(
            capturedIds.stream()
                .anyMatch(
                    workflowExecutionId -> workflowExecutionId.getJobPrincipalId() == 501L &&
                        workflowExecutionId.getTriggerName()
                            .equals("t_copy_mixed")),
            "copy-mode row must fire");

        Assertions.assertTrue(
            capturedIds.stream()
                .anyMatch(
                    workflowExecutionId -> workflowExecutionId.getJobPrincipalId() == 88L &&
                        workflowExecutionId.getTriggerName()
                            .equals("t_reference_mixed")),
            "reference-mode row must fire");
    }

    private static Workflow appEventWorkflow(String triggerName) {
        return new Workflow(
            "{\"label\":\"App Event Workflow\",\"triggers\":[{\"name\":\"" + triggerName
                + "\",\"type\":\"appEvent/v1/newEvent\"}],\"tasks\":[]}",
            Workflow.Format.JSON);
    }

    private static ConnectedUserProjectWorkflow referenceFor(
        String catalogWorkflowUuid, Long projectDeploymentId, boolean enabled, boolean dangling) {

        return new ConnectedUserProjectWorkflow(
            1L, 5L, null, 1, catalogWorkflowUuid, projectDeploymentId, enabled, dangling, null, 0);
    }

    private static ConnectedUserProjectWorkflow copyModeFor(
        Long projectWorkflowId, boolean enabled, boolean dangling) {

        return new ConnectedUserProjectWorkflow(
            2L, 5L, projectWorkflowId, 1, null, null, enabled, dangling, null, 0);
    }

    private AppEventTriggerApiController controller() {
        // EnvironmentService.getEnvironment(String) is a default method; delegate to the real implementation so a
        // null xEnvironment resolves to Environment.PRODUCTION as it does in production, instead of Mockito's
        // ordinary null default answer.
        EnvironmentService environmentService = mock(EnvironmentService.class, Mockito.CALLS_REAL_METHODS);

        // Bare-minimum stubs so a request that does reach doProcessTrigger does not NPE while building the
        // WebhookRequest.
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

        // Tests that only exercise the automation-bridge loop leave the integration-instance side empty.
        Mockito.lenient()
            .when(integrationInstanceService.getConnectedUserIntegrationInstances(Mockito.anyLong(), Mockito.eq(true)))
            .thenReturn(List.of());

        return new AppEventTriggerApiController(
            new ApplicationProperties(), connectedUserCodeWorkflowReferenceFacade, connectedUserService,
            environmentService, mock(FileEntryTokens.class), httpServletRequest, mock(HttpServletResponse.class),
            integrationInstanceConfigurationWorkflowService, integrationInstanceService,
            integrationInstanceWorkflowService, integrationWorkflowService, projectDeploymentService,
            projectWorkflowService, mock(TempFileStorage.class), webhookWorkflowExecutor, workflowService);
    }
}

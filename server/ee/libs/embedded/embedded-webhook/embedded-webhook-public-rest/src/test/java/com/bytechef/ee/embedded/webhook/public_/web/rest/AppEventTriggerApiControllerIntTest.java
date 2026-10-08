/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.webhook.public_.web.rest.config.EmbeddedWebhookPublicRestSharedMocks;
import com.bytechef.ee.embedded.webhook.public_.web.rest.config.EmbeddedWebhookPublicRestTestConfiguration;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedWebhookPublicRestTestConfiguration.class)
@TestPropertySource(properties = {
    "bytechef.edition=ee", "spring.mvc.problemdetails.enabled=true"
})
@WebMvcTest(AppEventTriggerApiController.class)
@EmbeddedWebhookPublicRestSharedMocks
class AppEventTriggerApiControllerIntTest {

    private static final String APP_EVENTS_PATH = "/v1/app-events";
    private static final String EXTERNAL_USER_APP_EVENTS_PATH = "/v1/{externalUserId}/app-events";

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @Autowired
    private WorkflowService workflowService;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();

        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any()))
            .thenReturn(new WebhookTriggerFlags(false, false, false, false));
    }

    @Test
    @WithMockUser(username = "user-1")
    void testEnabledReferenceWithAppEventTriggerIsIncludedInFanOut() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-1", 77L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-1"))
            .thenReturn("automation-workflow-1");
        when(workflowService.getWorkflow("automation-workflow-1"))
            .thenReturn(appEventWorkflow("t1"));

        postAppEvent();

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor).isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(77L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("automation-workflow-uuid-1", workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("t1", workflowExecutionId.getTriggerName());

        verify(webhookWorkflowExecutor).executeAsync(eq(workflowExecutionId), any(WebhookRequest.class));
    }

    @Test
    @WithMockUser(username = "user-1")
    void
        testAutomationBridgeUnsupportedOperationExceptionSkipsTheBridgeSourceWithoutAffectingIntegrationFanOut() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenThrow(new UnsupportedOperationException());

        stubIntegrationWorkflows(5L, "int-wf-1");

        postAppEvent();
        postAppEvent();

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor, times(2))
            .executeAsync(workflowExecutionIdCaptor.capture(), any(WebhookRequest.class));

        List<WorkflowExecutionId> workflowExecutionIds = workflowExecutionIdCaptor.getAllValues();

        Assertions.assertTrue(
            workflowExecutionIds.stream()
                .allMatch(workflowExecutionId -> workflowExecutionId.getType() == PlatformType.EMBEDDED &&
                    workflowExecutionId.getJobPrincipalId() == 5L));
    }

    @Test
    @WithMockUser(username = "user-1")
    void testDanglingReferenceIsSkipped() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-2", 78L, true, true)));

        postAppEvent();

        verifyNoInteractions(webhookWorkflowExecutor, projectWorkflowService);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testDisabledReferenceIsSkipped() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-3", 79L, false, false)));

        postAppEvent();

        verifyNoInteractions(webhookWorkflowExecutor, projectWorkflowService);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testReferenceWithoutAppEventTriggerIsSkipped() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-4", 80L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-4"))
            .thenReturn("automation-workflow-4");
        when(workflowService.getWorkflow("automation-workflow-4"))
            .thenReturn(
                new Workflow(
                    "{\"label\":\"No App Event\",\"triggers\":[{\"name\":\"t\",\"type\":\"request/v1\"}],"
                        + "\"tasks\":[]}",
                    Workflow.Format.JSON));

        postAppEvent();

        verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testFirstReferenceFailureDoesNotStopFanOutToRemainingReferences() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(
                List.of(
                    referenceFor("automation-workflow-uuid-fail", 90L, true, false),
                    referenceFor("automation-workflow-uuid-ok", 91L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-fail"))
            .thenThrow(new IllegalStateException("automation workflow lookup failed"));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-ok"))
            .thenReturn("automation-workflow-ok");
        when(workflowService.getWorkflow("automation-workflow-ok"))
            .thenReturn(appEventWorkflow("t_ok"));

        expectDispatchFailure("automation-workflow-uuid-fail");

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor).isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        Assertions.assertEquals(91L, workflowExecutionId.getJobPrincipalId());

        verify(webhookWorkflowExecutor).executeAsync(eq(workflowExecutionId), any(WebhookRequest.class));
    }

    @Test
    @WithMockUser(username = "login-a")
    void testCrossUserResolutionUsesOnlyTheCallersConnectedUserId() {
        stubConnectedUser("login-a", 10L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(10L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-a", 77L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-a"))
            .thenReturn("automation-workflow-a");
        when(workflowService.getWorkflow("automation-workflow-a"))
            .thenReturn(appEventWorkflow("t_a"));

        postAppEvent();

        verify(connectedUserService).getConnectedUser("login-a", Environment.PRODUCTION);
        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(10L);

        verify(connectedUserWorkflowReferenceFacade, never()).getConnectedUserWorkflows(20L);
        verify(connectedUserService, never()).getConnectedUser(eq("login-b"), any());
    }

    @Test
    @WithMockUser(username = "user-1")
    void testExistingIntegrationFanOutIsUnaffectedByAutomationBridgeReferences() {
        stubConnectedUser("user-1", 1L);

        IntegrationWorkflow integrationWorkflow = stubIntegrationWorkflows(5L, "int-wf-1").getFirst();

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-1", 77L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-1"))
            .thenReturn("automation-workflow-1");
        when(workflowService.getWorkflow("automation-workflow-1"))
            .thenReturn(appEventWorkflow("t_auto"));

        postAppEvent();

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor, times(2))
            .executeAsync(workflowExecutionIdCaptor.capture(), any(WebhookRequest.class));

        List<WorkflowExecutionId> workflowExecutionIds = workflowExecutionIdCaptor.getAllValues();

        Assertions.assertTrue(
            workflowExecutionIds.stream()
                .anyMatch(workflowExecutionId -> workflowExecutionId.getType() == PlatformType.EMBEDDED &&
                    workflowExecutionId.getJobPrincipalId() == 5L &&
                    Objects.equals(workflowExecutionId.getWorkflowUuid(), integrationWorkflow.getUuidAsString()) &&
                    Objects.equals(workflowExecutionId.getTriggerName(), "t_0")));

        Assertions.assertTrue(
            workflowExecutionIds.stream()
                .anyMatch(workflowExecutionId -> workflowExecutionId.getType() == PlatformType.AUTOMATION &&
                    workflowExecutionId.getJobPrincipalId() == 77L &&
                    Objects.equals(workflowExecutionId.getTriggerName(), "t_auto")));
    }

    @Test
    @WithMockUser(username = "user-1")
    void testCopyModeWorkflowWithAppEventTriggerFires() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copyModeFor(200L, true, false)));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(
            6L, 2, "copy-wf-1", UUID.fromString("00000000-0000-0000-0000-000000000201"));

        when(projectWorkflowService.getProjectWorkflow(200L))
            .thenReturn(projectWorkflow);
        when(projectDeploymentService.getProjectDeploymentId(6L, Environment.PRODUCTION))
            .thenReturn(500L);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(500L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-1"));
        when(workflowService.getWorkflow("copy-wf-1"))
            .thenReturn(appEventWorkflow("t_copy"));

        postAppEvent();

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor).isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(500L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals(projectWorkflow.getUuidAsString(), workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("t_copy", workflowExecutionId.getTriggerName());

        verify(webhookWorkflowExecutor).executeAsync(eq(workflowExecutionId), any(WebhookRequest.class));
    }

    @Test
    @WithMockUser(username = "user-1")
    void testMixedCopyAndReferenceWorkflowsBothFire() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copyModeFor(201L, true, false),
                referenceFor("automation-workflow-uuid-mixed", 88L, true, false)));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(
            7L, 2, "copy-wf-2", UUID.fromString("00000000-0000-0000-0000-000000000202"));

        when(projectWorkflowService.getProjectWorkflow(201L))
            .thenReturn(projectWorkflow);
        when(projectDeploymentService.getProjectDeploymentId(7L, Environment.PRODUCTION))
            .thenReturn(501L);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(501L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-2"));
        when(workflowService.getWorkflow("copy-wf-2"))
            .thenReturn(appEventWorkflow("t_copy_mixed"));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-mixed"))
            .thenReturn("automation-workflow-mixed");
        when(workflowService.getWorkflow("automation-workflow-mixed"))
            .thenReturn(appEventWorkflow("t_reference_mixed"));

        postAppEvent();

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor, times(2)).isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        List<WorkflowExecutionId> workflowExecutionIds = workflowExecutionIdCaptor.getAllValues();

        Assertions.assertTrue(
            workflowExecutionIds.stream()
                .anyMatch(workflowExecutionId -> workflowExecutionId.getJobPrincipalId() == 501L &&
                    Objects.equals(workflowExecutionId.getTriggerName(), "t_copy_mixed")),
            "copy-mode row must fire");
        Assertions.assertTrue(
            workflowExecutionIds.stream()
                .anyMatch(workflowExecutionId -> workflowExecutionId.getJobPrincipalId() == 88L &&
                    Objects.equals(workflowExecutionId.getTriggerName(), "t_reference_mixed")),
            "reference-mode row must fire");

        verify(webhookWorkflowExecutor, times(2)).executeAsync(any(WorkflowExecutionId.class),
            any(WebhookRequest.class));
    }

    @Test
    @WithMockUser(username = "user-1")
    void testJsonBodyIsDeliveredToEveryMatchingWorkflow() {
        stubConnectedUser("user-1", 1L);
        stubIntegrationWorkflows(5L, "int-wf-1", "int-wf-2");

        webTestClient
            .post()
            .uri(APP_EVENTS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"orderId\":42}")
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectBody()
            .isEmpty();

        ArgumentCaptor<WebhookRequest> webhookRequestCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(webhookWorkflowExecutor, times(2))
            .executeAsync(any(WorkflowExecutionId.class), webhookRequestCaptor.capture());

        for (WebhookRequest webhookRequest : webhookRequestCaptor.getAllValues()) {
            WebhookBodyImpl webhookBody = webhookRequest.body();

            Assertions.assertEquals(Map.of("orderId", 42), webhookBody.getContent());
        }
    }

    @Test
    @WithMockUser(username = "user-1")
    void testFailedBridgeDispatchIsReportedInsteadOfSucceeding() {
        stubConnectedUser("user-1", 1L);

        IntegrationWorkflow integrationWorkflow = stubIntegrationWorkflows(5L, "int-wf-1").getFirst();

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("automation-workflow-uuid-1", 77L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("automation-workflow-uuid-1"))
            .thenReturn("automation-workflow-1");
        when(workflowService.getWorkflow("automation-workflow-1"))
            .thenReturn(appEventWorkflow("t_auto"));

        doThrow(new IllegalStateException("broker unavailable"))
            .when(webhookWorkflowExecutor)
            .executeAsync(
                argThat(workflowExecutionId -> workflowExecutionId.getType() == PlatformType.AUTOMATION),
                any(WebhookRequest.class));

        expectDispatchFailure("automation-workflow-uuid-1");

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor, times(2))
            .executeAsync(workflowExecutionIdCaptor.capture(), any(WebhookRequest.class));

        List<WorkflowExecutionId> workflowExecutionIds = workflowExecutionIdCaptor.getAllValues();

        Assertions.assertTrue(
            workflowExecutionIds.stream()
                .anyMatch(workflowExecutionId -> Objects.equals(
                    workflowExecutionId.getWorkflowUuid(), integrationWorkflow.getUuidAsString())));
    }

    @Test
    @WithMockUser(username = "ext-1")
    void testExecuteWorkflowsForAnExternalUserIdFansOutForThatConnectedUser() {
        stubConnectedUser("ext-1", 1L);

        webTestClient
            .post()
            .uri(EXTERNAL_USER_APP_EVENTS_PATH, "ext-1")
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectBody()
            .isEmpty();

        verify(connectedUserService).getConnectedUser("ext-1", Environment.PRODUCTION);
        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(1L);
    }

    @Test
    @WithMockUser(username = "ext-1")
    void testExecuteWorkflowsRefusesAnExternalUserIdOtherThanTheAuthenticatedOne() {
        Throwable throwable = catchThrowable(() -> mockMvc.perform(post(EXTERNAL_USER_APP_EVENTS_PATH, "ext-2")));

        assertThat(throwable).hasCauseInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectedUserService, connectedUserWorkflowReferenceFacade, webhookWorkflowExecutor);
    }

    private void expectDispatchFailure(String... failedWorkflows) {
        webTestClient
            .post()
            .uri(APP_EVENTS_PATH)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(500)
            .jsonPath("$.title")
            .isEqualTo("App event dispatch failed")
            .jsonPath("$.detail")
            .isEqualTo("The app event could not be dispatched to workflows: " + String.join(", ", failedWorkflows))
            .jsonPath("$.failedWorkflows")
            .isEqualTo(List.of(failedWorkflows));
    }

    private void postAppEvent() {
        webTestClient
            .post()
            .uri(APP_EVENTS_PATH)
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectBody()
            .isEmpty();
    }

    private void stubConnectedUser(String login, long connectedUserId) {
        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), login + "@example.com", true, login, connectedUserId, login, 0);

        when(connectedUserService.getConnectedUser(eq(login), any()))
            .thenReturn(connectedUser);
    }

    private List<IntegrationWorkflow> stubIntegrationWorkflows(long integrationInstanceId, String... workflowIds) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(integrationInstanceId);

        when(integrationInstanceService.getConnectedUserIntegrationInstances(1L, true))
            .thenReturn(List.of(integrationInstance));

        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = new ArrayList<>();
        List<IntegrationWorkflow> integrationWorkflows = new ArrayList<>();

        for (int index = 0; index < workflowIds.length; index++) {
            String workflowId = workflowIds[index];
            long configurationWorkflowId = 30L + index;

            IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

            integrationInstanceWorkflow.setEnabled(true);
            integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(configurationWorkflowId);

            integrationInstanceWorkflows.add(integrationInstanceWorkflow);

            IntegrationInstanceConfigurationWorkflow configurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            configurationWorkflow.setWorkflowId(workflowId);

            when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                configurationWorkflowId))
                    .thenReturn(configurationWorkflow);
            when(workflowService.getWorkflow(workflowId))
                .thenReturn(appEventWorkflow("t_" + index));

            IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(
                integrationInstanceId, 1, workflowId, UUID.randomUUID());

            when(integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId))
                .thenReturn(integrationWorkflow);

            integrationWorkflows.add(integrationWorkflow);
        }

        when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId))
            .thenReturn(integrationInstanceWorkflows);

        return integrationWorkflows;
    }

    private static Workflow appEventWorkflow(String triggerName) {
        return new Workflow(
            "{\"label\":\"App Event Workflow\",\"triggers\":[{\"name\":\"" + triggerName
                + "\",\"type\":\"appEvent/v1/newEvent\"}],\"tasks\":[]}",
            Workflow.Format.JSON);
    }

    private static ConnectedUserProjectWorkflow copyModeFor(
        Long projectWorkflowId, boolean enabled, boolean dangling) {

        return new ConnectedUserProjectWorkflow(2L, 5L, projectWorkflowId, 1, null, null, enabled, dangling, null, 0);
    }

    private static ConnectedUserProjectWorkflow referenceFor(
        String automationWorkflowUuid, Long projectDeploymentId, boolean enabled, boolean dangling) {

        return new ConnectedUserProjectWorkflow(
            1L, 5L, null, 1, automationWorkflowUuid, projectDeploymentId, enabled, dangling, null, 0);
    }
}

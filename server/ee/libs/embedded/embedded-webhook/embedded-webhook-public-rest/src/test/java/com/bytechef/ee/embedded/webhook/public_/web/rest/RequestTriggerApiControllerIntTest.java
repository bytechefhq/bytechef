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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.webhook.public_.web.rest.config.EmbeddedWebhookPublicRestSharedMocks;
import com.bytechef.ee.embedded.webhook.public_.web.rest.config.EmbeddedWebhookPublicRestTestConfiguration;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
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
@WebMvcTest(RequestTriggerApiController.class)
@EmbeddedWebhookPublicRestSharedMocks
class RequestTriggerApiControllerIntTest {

    private static final String EXTERNAL_USER_WORKFLOW_PATH = "/v1/{externalUserId}/workflows/{workflowUuid}";
    private static final String WORKFLOW_PATH = "/v1/workflows/{workflowUuid}";

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

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
    void testIntegrationWorkflowResolutionIsUnchangedWhenOneExists() {
        stubConnectedUser("user-1", 1L);
        stubIntegrationWorkflow();

        expectDispatched(WORKFLOW_PATH, "uuid-1");

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(PlatformType.EMBEDDED, workflowExecutionId.getType());
        Assertions.assertEquals(2L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("uuid-1", workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_1", workflowExecutionId.getTriggerName());

        verifyNoInteractions(connectedUserWorkflowReferenceFacade);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testAutomationBridgeUnsupportedOperationExceptionReturnsNotFoundAndIsLoggedOnceAcrossTwoCalls() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-unsupported"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenThrow(new UnsupportedOperationException());

        expectNotFoundWithoutBody("uuid-unsupported");
        expectNotFoundWithoutBody("uuid-unsupported");

        verify(connectedUserWorkflowReferenceFacade, times(2)).getConnectedUserWorkflows(1L);
        verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testUnsupportedOperationExceptionPastTheReferenceFacadeIsNotReportedAsNotFound() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-copy"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copyModeFor(300L, true, false)));
        when(projectWorkflowService.getProjectWorkflow(300L))
            .thenThrow(new UnsupportedOperationException("project workflow lookup failed"));

        Throwable throwable = catchThrowable(() -> mockMvc.perform(post(WORKFLOW_PATH, "uuid-copy")));

        assertThat(throwable).hasCauseInstanceOf(UnsupportedOperationException.class);
        assertThat(throwable.getCause()).hasMessage("project workflow lookup failed");
    }

    @Test
    @WithMockUser(username = "user-1")
    void testUnknownWorkflowFallsThroughToTheAutomationBridge() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-2"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of());

        expectNotFoundWithoutBody("uuid-2");

        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(1L);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testExistingReferenceDispatchesAgainstTheAutomationWorkflowWithAutomationWorkflowExecutionId() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-3"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("uuid-3", 42L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("uuid-3"))
            .thenReturn("automation-workflow-3");
        when(workflowService.getWorkflow("automation-workflow-3"))
            .thenReturn(requestTriggerWorkflow("trigger_3"));

        expectDispatched(WORKFLOW_PATH, "uuid-3");

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(42L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("uuid-3", workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_3", workflowExecutionId.getTriggerName());

        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(1L);
        verifyNoMoreInteractions(connectedUserWorkflowReferenceFacade);
        verifyNoInteractions(projectDeploymentService);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testDisabledAndDanglingReferencesReturnByteIdenticalNotFoundResponses() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-5"), any()))
            .thenReturn(Optional.empty());
        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-6"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("uuid-5", 42L, false, false), referenceFor("uuid-6", 42L, true, true)));

        EntityExchangeResult<Void> disabledResult = expectNotFoundWithoutBody("uuid-5");
        EntityExchangeResult<Void> danglingResult = expectNotFoundWithoutBody("uuid-6");

        assertIdenticalResponses(disabledResult, danglingResult);

        verifyNoInteractions(webhookWorkflowExecutor);
        verify(projectWorkflowService, never()).getLastPublishedWorkflowId(anyString());
    }

    @Test
    @WithMockUser(username = "user-1")
    void testEnabledNonDanglingReferenceWithNullProjectDeploymentIdReturnsNotFound() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-8"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("uuid-8", null, true, false)));

        expectNotFoundWithoutBody("uuid-8");

        verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    @WithMockUser(username = "login-a")
    void testCrossUserResolutionUsesOnlyTheCallersConnectedUserIdentity() {
        stubConnectedUser("login-a", 10L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-9"), any()))
            .thenReturn(Optional.empty());
        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(10L))
            .thenReturn(List.of(referenceFor("uuid-9", 42L, true, false)));
        when(projectWorkflowService.getLastPublishedWorkflowId("uuid-9"))
            .thenReturn("automation-workflow-9");
        when(workflowService.getWorkflow("automation-workflow-9"))
            .thenReturn(requestTriggerWorkflow("trigger_9"));

        expectDispatched(WORKFLOW_PATH, "uuid-9");

        verify(connectedUserService).getConnectedUser("login-a", Environment.PRODUCTION);
        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(10L);

        verifyNoMoreInteractions(connectedUserWorkflowReferenceFacade);
        verify(connectedUserService, never()).getConnectedUser(eq("login-b"), any());
    }

    @Test
    @WithMockUser(username = "user-1")
    void testOwnCopyModeWorkflowUuidDispatchesDirectly() {
        stubConnectedUser("user-1", 1L);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copyModeFor(200L, true, false)));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(
            6L, 2, "copy-wf-1", UUID.fromString("00000000-0000-0000-0000-000000000201"));

        when(integrationWorkflowService.fetchLastWorkflowId(eq(projectWorkflow.getUuidAsString()), any()))
            .thenReturn(Optional.empty());
        when(projectWorkflowService.getProjectWorkflow(200L))
            .thenReturn(projectWorkflow);
        when(projectDeploymentService.getProjectDeploymentId(6L, Environment.PRODUCTION))
            .thenReturn(500L);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(500L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-1"));
        when(workflowService.getWorkflow("copy-wf-1"))
            .thenReturn(requestTriggerWorkflow("trigger_copy"));

        expectDispatched(WORKFLOW_PATH, projectWorkflow.getUuidAsString());

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(500L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals(projectWorkflow.getUuidAsString(), workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_copy", workflowExecutionId.getTriggerName());

        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(1L);
        verifyNoMoreInteractions(connectedUserWorkflowReferenceFacade);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testOwnCopyResolutionWinsOverAReferenceAndACopyOfTheSameUuid() {
        stubConnectedUser("user-1", 1L);

        ProjectWorkflow ownProjectWorkflow = new ProjectWorkflow(
            10L, 2, "own-wf", UUID.fromString("00000000-0000-0000-0000-000000000401"));
        ProjectWorkflow otherProjectWorkflow = new ProjectWorkflow(
            11L, 2, "other-wf", UUID.fromString("00000000-0000-0000-0000-000000000402"));

        String workflowUuid = ownProjectWorkflow.getUuidAsString();

        ConnectedUserProjectWorkflow otherCopy = copyModeFor(401L, true, false);

        otherCopy.setCopiedFromWorkflowUuid(workflowUuid);

        ConnectedUserProjectWorkflow ownCopy = copyModeFor(400L, true, false);

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor(workflowUuid, 42L, true, false), otherCopy, ownCopy));
        when(integrationWorkflowService.fetchLastWorkflowId(eq(workflowUuid), any()))
            .thenReturn(Optional.empty());
        when(projectWorkflowService.getProjectWorkflow(401L))
            .thenReturn(otherProjectWorkflow);
        when(projectWorkflowService.getProjectWorkflow(400L))
            .thenReturn(ownProjectWorkflow);
        when(projectDeploymentService.getProjectDeploymentId(10L, Environment.PRODUCTION))
            .thenReturn(700L);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(700L, workflowUuid))
            .thenReturn(Optional.of("own-wf"));
        when(workflowService.getWorkflow("own-wf"))
            .thenReturn(requestTriggerWorkflow("trigger_own"));

        expectDispatched(WORKFLOW_PATH, workflowUuid);

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(700L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals(workflowUuid, workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_own", workflowExecutionId.getTriggerName());

        verify(projectWorkflowService, never()).getLastPublishedWorkflowId(anyString());
        verify(projectDeploymentService, never()).getProjectDeploymentId(eq(11L), any());
    }

    @Test
    @WithMockUser(username = "user-1")
    void testReferenceResolutionWinsOverACopyMadeFromTheSameTemplate() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("template-uuid-5"), any()))
            .thenReturn(Optional.empty());

        ConnectedUserProjectWorkflow copyOfTemplate = copyModeFor(500L, true, false);

        copyOfTemplate.setCopiedFromWorkflowUuid("template-uuid-5");

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(copyOfTemplate, referenceFor("template-uuid-5", 43L, true, false)));
        when(projectWorkflowService.getProjectWorkflow(500L))
            .thenReturn(
                new ProjectWorkflow(12L, 2, "copy-wf-5", UUID.fromString("00000000-0000-0000-0000-000000000501")));
        when(projectWorkflowService.getLastPublishedWorkflowId("template-uuid-5"))
            .thenReturn("automation-workflow-5");
        when(workflowService.getWorkflow("automation-workflow-5"))
            .thenReturn(requestTriggerWorkflow("trigger_reference"));

        expectDispatched(WORKFLOW_PATH, "template-uuid-5");

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(43L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals("template-uuid-5", workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_reference", workflowExecutionId.getTriggerName());

        verifyNoInteractions(projectDeploymentService);
    }

    @Test
    @WithMockUser(username = "user-1")
    void testAutomationWorkflowUuidOfAnExistingCopyDispatchesThatCopy() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("template-uuid-2"), any()))
            .thenReturn(Optional.empty());

        ConnectedUserProjectWorkflow existingCopy = copyModeFor(301L, true, false);

        existingCopy.setCopiedFromWorkflowUuid("template-uuid-2");

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(existingCopy));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(
            8L, 2, "copy-wf-3", UUID.fromString("00000000-0000-0000-0000-000000000302"));

        when(projectWorkflowService.getProjectWorkflow(301L))
            .thenReturn(projectWorkflow);
        when(projectDeploymentService.getProjectDeploymentId(8L, Environment.PRODUCTION))
            .thenReturn(601L);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(601L, projectWorkflow.getUuidAsString()))
            .thenReturn(Optional.of("copy-wf-3"));
        when(workflowService.getWorkflow("copy-wf-3"))
            .thenReturn(requestTriggerWorkflow("trigger_existing"));

        expectDispatched(WORKFLOW_PATH, "template-uuid-2");

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(PlatformType.AUTOMATION, workflowExecutionId.getType());
        Assertions.assertEquals(601L, workflowExecutionId.getJobPrincipalId());
        Assertions.assertEquals(projectWorkflow.getUuidAsString(), workflowExecutionId.getWorkflowUuid());
        Assertions.assertEquals("trigger_existing", workflowExecutionId.getTriggerName());

        verify(connectedUserWorkflowReferenceFacade).getConnectedUserWorkflows(1L);
        verifyNoMoreInteractions(connectedUserWorkflowReferenceFacade);
        verify(projectWorkflowService, never()).getLastPublishedWorkflowId(anyString());
    }

    @Test
    @WithMockUser(username = "user-1")
    void testUnactivatedAutomationWorkflowTemplateReturnsTheSameNotFoundAsAnUnknownUuidAndProvisionsNothing() {
        stubConnectedUser("user-1", 1L);

        when(integrationWorkflowService.fetchLastWorkflowId(eq("template-uuid-3"), any()))
            .thenReturn(Optional.empty());
        when(integrationWorkflowService.fetchLastWorkflowId(eq("unknown-uuid-3"), any()))
            .thenReturn(Optional.empty());

        ConnectedUserProjectWorkflow unrelatedCopy = copyModeFor(303L, true, false);

        unrelatedCopy.setCopiedFromWorkflowUuid("other-template-uuid");

        when(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(1L))
            .thenReturn(List.of(referenceFor("other-automation-workflow-uuid", 42L, true, false), unrelatedCopy));
        when(projectWorkflowService.getProjectWorkflow(303L))
            .thenReturn(
                new ProjectWorkflow(
                    9L, 2, "copy-wf-unrelated", UUID.fromString("00000000-0000-0000-0000-000000000303")));

        EntityExchangeResult<Void> templateResult = expectNotFoundWithoutBody("template-uuid-3");
        EntityExchangeResult<Void> unknownResult = expectNotFoundWithoutBody("unknown-uuid-3");

        assertIdenticalResponses(templateResult, unknownResult);

        verify(connectedUserWorkflowReferenceFacade, times(2)).getConnectedUserWorkflows(1L);
        verifyNoMoreInteractions(connectedUserWorkflowReferenceFacade);
        verifyNoInteractions(projectDeploymentService, webhookWorkflowExecutor);
        verify(projectWorkflowService, never()).getLastPublishedWorkflowId(anyString());
        verify(workflowService, never()).getWorkflow(anyString());
    }

    @Test
    @WithMockUser(username = "ext-1")
    void testExecuteWorkflowForAnExternalUserIdRunsAsThatConnectedUser() {
        stubConnectedUser("ext-1", 1L);
        stubIntegrationWorkflow();

        expectDispatched(EXTERNAL_USER_WORKFLOW_PATH, "ext-1", "uuid-1");

        verify(connectedUserService).getConnectedUser("ext-1", Environment.PRODUCTION);
        verify(integrationInstanceService).getIntegrationInstance(1L, "integration-wf-1", Environment.PRODUCTION);

        WorkflowExecutionId workflowExecutionId = captureDispatchedWorkflowExecutionId();

        Assertions.assertEquals(PlatformType.EMBEDDED, workflowExecutionId.getType());
        Assertions.assertEquals(2L, workflowExecutionId.getJobPrincipalId());
    }

    @Test
    @WithMockUser(username = "ext-1")
    void testExecuteWorkflowRefusesAnExternalUserIdOtherThanTheAuthenticatedOne() {
        Throwable throwable = catchThrowable(
            () -> mockMvc.perform(post(EXTERNAL_USER_WORKFLOW_PATH, "ext-2", "uuid-1")));

        assertThat(throwable).hasCauseInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(
            connectedUserService, integrationWorkflowService, connectedUserWorkflowReferenceFacade,
            webhookWorkflowExecutor);
    }

    private static void assertIdenticalResponses(
        EntityExchangeResult<Void> firstResult, EntityExchangeResult<Void> secondResult) {

        Assertions.assertEquals(firstResult.getStatus(), secondResult.getStatus());
        Assertions.assertEquals(firstResult.getResponseHeaders(), secondResult.getResponseHeaders());
        Assertions.assertArrayEquals(firstResult.getResponseBodyContent(), secondResult.getResponseBodyContent());
    }

    private WorkflowExecutionId captureDispatchedWorkflowExecutionId() {
        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor).isWorkflowDisabled(workflowExecutionIdCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdCaptor.getValue();

        verify(webhookWorkflowExecutor).executeAsync(eq(workflowExecutionId), any(WebhookRequest.class));

        return workflowExecutionId;
    }

    private void expectDispatched(String path, Object... uriVariables) {
        webTestClient
            .post()
            .uri(path, uriVariables)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .isEmpty();
    }

    private EntityExchangeResult<Void> expectNotFoundWithoutBody(String workflowUuid) {
        return webTestClient
            .post()
            .uri(WORKFLOW_PATH, workflowUuid)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .isEmpty();
    }

    private void stubConnectedUser(String login, long connectedUserId) {
        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), login + "@example.com", true, login, connectedUserId, login, 0);

        when(connectedUserService.getConnectedUser(eq(login), any()))
            .thenReturn(connectedUser);
    }

    private void stubIntegrationWorkflow() {
        when(integrationWorkflowService.fetchLastWorkflowId(eq("uuid-1"), any()))
            .thenReturn(Optional.of("integration-wf-1"));

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(2L);

        when(integrationInstanceService.getIntegrationInstance(1L, "integration-wf-1", Environment.PRODUCTION))
            .thenReturn(integrationInstance);
        when(workflowService.getWorkflow("integration-wf-1"))
            .thenReturn(
                new Workflow(
                    "{\"label\":\"Integration Workflow\",\"triggers\":[{\"name\":\"trigger_1\",\"type\":\"request/v1\"}],"
                        + "\"tasks\":[]}",
                    Workflow.Format.JSON));
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

    private static Workflow requestTriggerWorkflow(String triggerName) {
        return new Workflow(
            "{\"label\":\"Automation Workflow\",\"triggers\":[{\"name\":\"" + triggerName
                + "\",\"type\":\"request/v1\"}],"
                + "\"tasks\":[]}",
            Workflow.Format.JSON);
    }
}

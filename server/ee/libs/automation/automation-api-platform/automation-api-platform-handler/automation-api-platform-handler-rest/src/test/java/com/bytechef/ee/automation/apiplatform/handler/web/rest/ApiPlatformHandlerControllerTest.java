/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApiPlatformHandlerControllerTest {

    private static final long CRM_DEPLOYMENT_ID = 10L;
    private static final long SEARCH_WORKFLOW_DEPLOYMENT_ID = 101L;
    private static final long CONTACT_WORKFLOW_DEPLOYMENT_ID = 102L;

    private final ApiCollectionEndpointService apiCollectionEndpointService = mock(ApiCollectionEndpointService.class);
    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WebhookWorkflowExecutor webhookWorkflowExecutor = mock(WebhookWorkflowExecutor.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private ApiPlatformHandlerController apiPlatformHandlerController;

    @BeforeEach
    void beforeEach() {
        apiPlatformHandlerController = new ApiPlatformHandlerController(
            apiCollectionService, apiCollectionEndpointService, mock(ApplicationProperties.class),
            projectDeploymentService, projectDeploymentWorkflowService,
            projectWorkflowService, mock(TempFileStorage.class), webhookWorkflowExecutor, workflowService);

        when(apiCollectionService.getApiCollections(isNull(), any(Environment.class), isNull(), isNull()))
            .thenReturn(List.of(apiCollection(1L, "crm", CRM_DEPLOYMENT_ID)));
        when(apiCollectionEndpointService.getApiEndpoints(1L))
            .thenReturn(
                List.of(
                    apiCollectionEndpoint("getContact", "contacts/{id}", CONTACT_WORKFLOW_DEPLOYMENT_ID),
                    apiCollectionEndpoint("searchContacts", "contacts/search", SEARCH_WORKFLOW_DEPLOYMENT_ID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(SEARCH_WORKFLOW_DEPLOYMENT_ID))
            .thenReturn(projectDeploymentWorkflow("searchWorkflow", true));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(CONTACT_WORKFLOW_DEPLOYMENT_ID))
            .thenReturn(projectDeploymentWorkflow("contactWorkflow", true));
        when(projectDeploymentService.getProjectDeployment(CRM_DEPLOYMENT_ID))
            .thenReturn(projectDeployment(CRM_DEPLOYMENT_ID, true));
        when(projectWorkflowService.getProjectWorkflowUuid(eq(CRM_DEPLOYMENT_ID), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(1) + "-uuid");
        when(workflowService.getWorkflow(anyString()))
            .thenAnswer(invocation -> workflow());
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Map.of("result", "ok")));
    }

    @Test
    void testMatchesFirstEndpointInCollectionOrder() {
        ResponseEntity<?> responseEntity = apiPlatformHandlerController.handleGetMethod(
            request("GET", "/api/o/v1/crm/contacts/search"), new MockHttpServletResponse());

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(200);
        assertThat(captureWorkflowExecutionId().getWorkflowUuid()).isEqualTo("contactWorkflow-uuid");
    }

    @Test
    void testPathVariablesMergedIntoParameters() {
        MockHttpServletRequest httpServletRequest = request("GET", "/api/o/v1/crm/contacts/42");

        httpServletRequest.addParameter("expand", "owner");

        apiPlatformHandlerController.handleGetMethod(httpServletRequest, new MockHttpServletResponse());

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);
        ArgumentCaptor<WebhookRequest> webhookRequestArgumentCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(webhookWorkflowExecutor).executeSync(
            workflowExecutionIdArgumentCaptor.capture(), webhookRequestArgumentCaptor.capture());

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdArgumentCaptor.getValue();
        WebhookRequest webhookRequest = webhookRequestArgumentCaptor.getValue();

        assertThat(workflowExecutionId.getWorkflowUuid()).isEqualTo("contactWorkflow-uuid");
        assertThat(workflowExecutionId.getJobPrincipalId()).isEqualTo(CRM_DEPLOYMENT_ID);
        assertThat(workflowExecutionId.getTriggerName()).isEqualTo("trigger_1");
        assertThat(webhookRequest.parameters())
            .containsEntry("id", List.of("42"))
            .containsEntry("expand", List.of("owner"));
    }

    @Test
    void testDisabledWorkflowReturns404() {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(CONTACT_WORKFLOW_DEPLOYMENT_ID))
            .thenReturn(projectDeploymentWorkflow("contactWorkflow", false));

        ResponseEntity<?> responseEntity = apiPlatformHandlerController.handleGetMethod(
            request("GET", "/api/o/v1/crm/contacts/42"), new MockHttpServletResponse());

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(404);

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testNoMatchReturns404() {
        ResponseEntity<?> responseEntity = apiPlatformHandlerController.handleGetMethod(
            request("GET", "/api/o/v1/billing/invoices"), new MockHttpServletResponse());

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(404);

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testEnvironmentDefaultsToProduction() {
        apiPlatformHandlerController.handleGetMethod(
            request("GET", "/api/o/v1/crm/contacts/42"), new MockHttpServletResponse());

        verify(apiCollectionService).getApiCollections(null, Environment.PRODUCTION, null, null);

        MockHttpServletRequest httpServletRequest = request("GET", "/api/o/v1/crm/contacts/42");

        httpServletRequest.addHeader("x-environment", "development");

        apiPlatformHandlerController.handleGetMethod(httpServletRequest, new MockHttpServletResponse());

        verify(apiCollectionService).getApiCollections(null, Environment.DEVELOPMENT, null, null);
    }

    private WorkflowExecutionId captureWorkflowExecutionId() {
        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor).executeSync(workflowExecutionIdArgumentCaptor.capture(), any());

        return workflowExecutionIdArgumentCaptor.getValue();
    }

    private static ApiCollection apiCollection(long id, String contextPath, long projectDeploymentId) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(1);
        apiCollection.setContextPath(contextPath);
        apiCollection.setId(id);
        apiCollection.setName(contextPath);
        apiCollection.setProjectDeploymentId(projectDeploymentId);

        return apiCollection;
    }

    private static ApiCollectionEndpoint apiCollectionEndpoint(
        String name, String path, long projectDeploymentWorkflowId) {

        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
        apiCollectionEndpoint.setId(projectDeploymentWorkflowId + 1000);
        apiCollectionEndpoint.setName(name);
        apiCollectionEndpoint.setPath(path);
        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflowId);

        return apiCollectionEndpoint;
    }

    private static ProjectDeployment projectDeployment(long id, boolean enabled) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(enabled);
        projectDeployment.setId(id);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(String workflowId, boolean enabled) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static MockHttpServletRequest request(String method, String requestURI) {
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(method, requestURI);

        httpServletRequest.setContentType("application/json");

        return httpServletRequest;
    }

    private static Workflow workflow() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), any()))
            .thenReturn(
                List.of(new WorkflowTrigger(Map.of("name", "trigger_1", "type", "apiPlatform/v1/newApiRequest"))));

        return workflow;
    }
}

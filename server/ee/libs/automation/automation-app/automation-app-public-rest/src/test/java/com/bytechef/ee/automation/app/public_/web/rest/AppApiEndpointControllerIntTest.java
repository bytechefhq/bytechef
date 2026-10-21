/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppExecutionRestTestConfiguration;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.exception.AbstractErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AppExecutionRestTestConfiguration.class)
@ExtendWith(ObjectMapperSetupExtension.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppApiEndpointController.class)
class AppApiEndpointControllerIntTest {

    private static final long CRM_DEPLOYMENT_ID = 10L;
    private static final long CREATE_CONTACT_WORKFLOW_ID = 31L;
    private static final long GET_CONTACT_WORKFLOW_ID = 32L;
    private static final long SEARCH_CONTACTS_WORKFLOW_ID = 33L;

    private static final AppPrincipal APP_PRINCIPAL = new AppPrincipal("public", 5L, 7L, Environment.DEVELOPMENT);

    @MockitoBean
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Autowired
    private AppApiEndpointController appApiEndpointController;

    @MockitoBean
    private AppApiExecutionFacade appApiExecutionFacade;

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectDeploymentService projectDeploymentService;

    @MockitoBean
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @MockitoBean
    private ProjectWorkflowService projectWorkflowService;

    @MockitoBean
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @MockitoBean
    private WorkflowService workflowService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        TestSecurityContextHolder.setAuthentication(new TestingAuthenticationToken(APP_PRINCIPAL, null, "ROLE_APP"));

        ApiCollection crmApiCollection = apiCollection(21L, "crm");

        when(appApiExecutionFacade.getCallableApiCollections()).thenReturn(List.of(crmApiCollection));
        when(appApiExecutionFacade.getUnavailableApiCollections()).thenReturn(List.of());
        when(appApiExecutionFacade.recordApiCall(anyString(), anyString(), any(Supplier.class), any()))
            .thenAnswer(invocation -> ((Supplier<Object>) invocation.getArgument(2)).get());
        when(apiCollectionEndpointService.getApiEndpoints(21L)).thenReturn(
            List.of(
                endpoint("createContact", HttpMethod.POST, "contacts", CREATE_CONTACT_WORKFLOW_ID),
                endpoint("getContact", HttpMethod.GET, "contacts/{id}", GET_CONTACT_WORKFLOW_ID),
                endpoint("searchContacts", HttpMethod.GET, "contacts/search", SEARCH_CONTACTS_WORKFLOW_ID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(anyLong()))
            .thenAnswer(invocation -> projectDeploymentWorkflow("workflow-" + invocation.getArgument(0), true));
        when(projectDeploymentService.getProjectDeployment(CRM_DEPLOYMENT_ID))
            .thenReturn(projectDeployment());
        when(projectWorkflowService.getProjectWorkflowUuid(eq(CRM_DEPLOYMENT_ID), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(1) + "-uuid");
        when(workflowService.getWorkflow(anyString())).thenAnswer(invocation -> workflow());
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));

        stubWorkflowResponse(WebhookResponse.json(Map.of("id", 7), Map.of(), 201));
    }

    @AfterEach
    void afterEach() {
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void testBoundCollectionEndpointRuns() throws Exception {
        mockMvc.perform(
            post("/api/app/v1/api/v1/crm/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ada\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7));

        assertThat(captureWorkflowExecutionId().getWorkflowUuid()).isEqualTo(
            "workflow-" + CREATE_CONTACT_WORKFLOW_ID + "-uuid");
    }

    @Test
    void testWorkflowChosenStatusAndHeadersPassThrough() throws Exception {
        stubWorkflowResponse(WebhookResponse.json(Map.of("error", "name is required"), Map.of("X-Foo", "bar"), 400));

        mockMvc.perform(
            post("/api/app/v1/api/v1/crm/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(header().string("X-Foo", "bar"))
            .andExpect(header().doesNotExist("X-ByteChef-App-Error"))
            .andExpect(jsonPath("$.error").value("name is required"))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void testWorkflowChosenUnauthorizedBodyIsNotMarkedAsAppError() throws Exception {
        stubWorkflowResponse(
            WebhookResponse.json(Map.of("code", "UNAUTHORIZED", "message", "no session"), Map.of(), 401));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("X-ByteChef-App-Error"))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testWorkflowCannotMarkItsResponseAsAppError() throws Exception {
        stubWorkflowResponse(
            WebhookResponse.json(
                Map.of("code", "APP_DISABLED"), Map.of("x-bytechef-app-error", "true", "X-Foo", "bar"), 403));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isForbidden())
            .andExpect(header().string("X-Foo", "bar"))
            .andExpect(header().doesNotExist("X-ByteChef-App-Error"))
            .andExpect(jsonPath("$.code").value("APP_DISABLED"));
    }

    @Test
    void testFailedWorkflowReturnsMarked500() throws Exception {
        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenThrow(new IllegalStateException("Job failed"));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isInternalServerError())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_EXECUTION_FAILED"));
    }

    @Test
    void testTimedOutWorkflowReturnsMarked504() throws Exception {
        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenThrow(new RuntimeException(new TimeoutException("Job did not complete")));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isGatewayTimeout())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_EXECUTION_TIMEOUT"));
    }

    @Test
    void testMissingApiPlatformTriggerReturnsMarked500() throws Exception {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), any()))
            .thenReturn(List.of());
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isInternalServerError())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_EXECUTION_FAILED"));

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testComponentFailureReturnsMarked422() throws Exception {
        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenThrow(
                new ExecutionException("perform failed", new AbstractErrorType(ActionDefinition.class, 104) {}));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("COMPONENT_ERROR"))
            .andExpect(jsonPath("$.message").value("perform failed"));
    }

    @Test
    void testUnboundCollectionReturns404() throws Exception {
        when(appApiExecutionFacade.getCallableApiCollections()).thenReturn(List.of());

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_API_ENDPOINT_NOT_FOUND"));

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testOtherAppsCollectionReturns404() throws Exception {
        when(apiCollectionEndpointService.getApiEndpoints(22L)).thenReturn(
            List.of(endpoint("listInvoices", HttpMethod.GET, "invoices", 41L)));

        mockMvc.perform(get("/api/app/v1/api/v1/billing/invoices"))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_API_ENDPOINT_NOT_FOUND"));

        verify(apiCollectionEndpointService, never()).getApiEndpoints(22L);
        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testDisabledEndpointWorkflowReturns409() throws Exception {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(GET_CONTACT_WORKFLOW_ID))
            .thenReturn(projectDeploymentWorkflow("workflow-" + GET_CONTACT_WORKFLOW_ID, false));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isConflict())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_API_COLLECTION_UNAVAILABLE"));

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testUnavailableBoundCollectionReturns409() throws Exception {
        when(appApiExecutionFacade.getCallableApiCollections()).thenReturn(List.of());
        when(appApiExecutionFacade.getUnavailableApiCollections()).thenReturn(List.of(apiCollection(21L, "crm")));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isConflict())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_API_COLLECTION_UNAVAILABLE"));

        mockMvc.perform(get("/api/app/v1/api/v1/crmx/contacts/42"))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_API_ENDPOINT_NOT_FOUND"));

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testOverlappingPatternsUseFirstMatch() throws Exception {
        ApiCollection firstApiCollection = apiCollection(21L, "crm");
        ApiCollection secondApiCollection = apiCollection(22L, "crm");

        when(appApiExecutionFacade.getCallableApiCollections())
            .thenReturn(List.of(firstApiCollection, secondApiCollection));
        when(apiCollectionEndpointService.getApiEndpoints(21L)).thenReturn(
            List.of(endpoint("getContact", HttpMethod.GET, "contacts/{id}", GET_CONTACT_WORKFLOW_ID)));
        when(apiCollectionEndpointService.getApiEndpoints(22L)).thenReturn(
            List.of(endpoint("searchContacts", HttpMethod.GET, "contacts/search", SEARCH_CONTACTS_WORKFLOW_ID)));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/search"))
            .andExpect(status().isCreated());

        assertThat(captureWorkflowExecutionId().getWorkflowUuid()).isEqualTo(
            "workflow-" + GET_CONTACT_WORKFLOW_ID + "-uuid");

        verify(appApiExecutionFacade).recordApiCall(eq("crm"), eq("getContact"), any(), any());
    }

    @Test
    void testXEnvironmentHeaderIsIgnored() throws Exception {
        mockMvc.perform(
            get("/api/app/v1/api/v1/crm/contacts/42")
                .header("X-ENVIRONMENT", "PRODUCTION"))
            .andExpect(status().isCreated());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(
            "GET", "/api/app/v1/api/v1/crm/contacts/42");

        httpServletRequest.addHeader("X-ENVIRONMENT", "PRODUCTION");

        assertThat(appApiEndpointController.getEnvironment(httpServletRequest)).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testCallIsRecordedWithAppId() throws Exception {
        mockMvc.perform(
            post("/api/app/v1/api/v1/crm/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated());

        verify(appApiExecutionFacade).recordApiCall(eq("crm"), eq("createContact"), any(), any());
    }

    @Test
    void testDisabledAppReturns403() throws Exception {
        when(appApiExecutionFacade.getCallableApiCollections())
            .thenThrow(new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED));

        mockMvc.perform(get("/api/app/v1/api/v1/crm/contacts/42"))
            .andExpect(status().isForbidden())
            .andExpect(header().string("X-ByteChef-App-Error", "true"))
            .andExpect(jsonPath("$.code").value("APP_DISABLED"));

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    private WorkflowExecutionId captureWorkflowExecutionId() {
        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(webhookWorkflowExecutor).executeSync(workflowExecutionIdArgumentCaptor.capture(), any());

        return workflowExecutionIdArgumentCaptor.getValue();
    }

    private void stubWorkflowResponse(WebhookResponse webhookResponse) {
        when(webhookWorkflowExecutor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponse));
    }

    private static ApiCollection apiCollection(long id, String contextPath) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(1);
        apiCollection.setContextPath(contextPath);
        apiCollection.setId(id);
        apiCollection.setName(contextPath);
        apiCollection.setProjectDeploymentId(CRM_DEPLOYMENT_ID);

        return apiCollection;
    }

    private static ApiCollectionEndpoint endpoint(
        String name, HttpMethod httpMethod, String path, long projectDeploymentWorkflowId) {

        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setHttpMethod(httpMethod);
        apiCollectionEndpoint.setName(name);
        apiCollectionEndpoint.setPath(path);
        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflowId);

        return apiCollectionEndpoint;
    }

    private static ProjectDeployment projectDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setId(CRM_DEPLOYMENT_ID);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(String workflowId, boolean enabled) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static Workflow workflow() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), any()))
            .thenReturn(
                List.of(new WorkflowTrigger(Map.of("name", "trigger_1", "type", "apiPlatform/v1/newApiRequest"))));

        return workflow;
    }
}

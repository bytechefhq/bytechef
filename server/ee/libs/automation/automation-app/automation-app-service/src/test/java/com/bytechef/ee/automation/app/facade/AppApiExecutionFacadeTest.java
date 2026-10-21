/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppApiCollectionDTO;
import com.bytechef.ee.automation.app.dto.AppApiEndpointDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.workflow.AppApiCollectionResolver;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AppApiExecutionFacadeTest {

    private static final long APP_ID = 5L;
    private static final String TENANT_ID = "public";
    private static final long WORKSPACE_ID = 7L;

    private final ApiCollectionEndpointService apiCollectionEndpointService = mock(ApiCollectionEndpointService.class);
    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final AppApiCollectionResolver appApiCollectionResolver = mock(AppApiCollectionResolver.class);
    private final AppApiCollectionService appApiCollectionService = mock(AppApiCollectionService.class);
    private final AppService appService = mock(AppService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private AppApiExecutionFacade appApiExecutionFacade;

    @BeforeEach
    void beforeEach() {
        appApiExecutionFacade = new AppApiExecutionFacadeImpl(
            apiCollectionEndpointService, apiCollectionService, appApiCollectionResolver, appApiCollectionService,
            appService, projectDeploymentWorkflowService, toolExecutionRecorder, workflowService);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new TestingAuthenticationToken(
                    new AppPrincipal(TENANT_ID, APP_ID, WORKSPACE_ID, Environment.DEVELOPMENT), null));

        when(appService.getApp(APP_ID)).thenReturn(app(AppStatus.ENABLED));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCallableCollectionsExcludeUnavailable() {
        ApiCollection crmApiCollection = apiCollection(21L, "crm");
        ApiCollection billingApiCollection = apiCollection(22L, "billing");
        ApiCollection supportApiCollection = apiCollection(23L, "support");

        when(appApiCollectionService.getBindings(APP_ID)).thenReturn(
            List.of(binding(21L), binding(22L), binding(23L)));
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.DEVELOPMENT, 21L))
            .thenReturn(Optional.of(crmApiCollection));
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.DEVELOPMENT, 22L))
            .thenReturn(Optional.empty());
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.DEVELOPMENT, 23L))
            .thenReturn(Optional.of(supportApiCollection));
        when(apiCollectionService.fetchApiCollection(22L)).thenReturn(Optional.of(billingApiCollection));

        assertThat(appApiExecutionFacade.getCallableApiCollections())
            .containsExactly(crmApiCollection, supportApiCollection);
        assertThat(appApiExecutionFacade.getUnavailableApiCollections()).containsExactly(billingApiCollection);

        verify(appApiCollectionService, times(2)).getBindings(APP_ID);
    }

    @Test
    void testDisabledAppThrows() {
        when(appService.getApp(APP_ID)).thenReturn(app(AppStatus.DISABLED));

        assertThatThrownBy(() -> appApiExecutionFacade.getCallableApiCollections())
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_DISABLED.getErrorKey());
        assertThatThrownBy(() -> appApiExecutionFacade.getApiCollections())
            .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(
            () -> appApiExecutionFacade.recordApiCall("crm", "getContact", () -> "called", called -> 200))
                .isInstanceOf(ConfigurationException.class);

        verify(appApiCollectionService, never()).getBindings(APP_ID);
    }

    @Test
    void testSchemasNullWhenTriggerUnreadable() {
        ApiCollection crmApiCollection = apiCollection(21L, "crm");

        when(appApiCollectionService.getBindings(APP_ID)).thenReturn(List.of(binding(21L), binding(99L)));
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.DEVELOPMENT, 21L))
            .thenReturn(Optional.of(crmApiCollection));
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.DEVELOPMENT, 99L))
            .thenReturn(Optional.empty());
        when(apiCollectionService.fetchApiCollection(99L)).thenReturn(Optional.empty());
        when(apiCollectionEndpointService.getApiEndpoints(21L)).thenReturn(
            List.of(
                endpoint("getContact", HttpMethod.GET, "contacts/{id}", 31L),
                endpoint("createContact", HttpMethod.POST, "contacts", 32L),
                endpoint("deleteContact", HttpMethod.DELETE, "contacts/{id}", 33L)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(31L))
            .thenReturn(projectDeploymentWorkflow("getContactWorkflow"));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(32L))
            .thenReturn(projectDeploymentWorkflow("createContactWorkflow"));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(33L))
            .thenReturn(projectDeploymentWorkflow("deleteContactWorkflow"));
        when(workflowService.fetchWorkflow("getContactWorkflow")).thenAnswer(invocation -> Optional.of(
            workflow(
                Map.of(
                    "request", Map.of("body", "{\"type\": \"object\"}"),
                    "response", Map.of("success", "{\"type\": \"string\"}")))));
        when(workflowService.fetchWorkflow("createContactWorkflow")).thenAnswer(invocation -> Optional.of(
            workflow(Map.of("request", Map.of("body", "not json"), "response", "not a map"))));
        when(workflowService.fetchWorkflow("deleteContactWorkflow")).thenReturn(Optional.empty());

        List<AppApiCollectionDTO> appApiCollectionDTOs = appApiExecutionFacade.getApiCollections();

        assertThat(appApiCollectionDTOs).hasSize(2);

        AppApiCollectionDTO crmApiCollectionDTO = appApiCollectionDTOs.getFirst();

        assertThat(crmApiCollectionDTO.available()).isTrue();
        assertThat(crmApiCollectionDTO.contextPath()).isEqualTo("crm");
        assertThat(crmApiCollectionDTO.collectionVersion()).isEqualTo(1);
        assertThat(crmApiCollectionDTO.endpoints())
            .containsExactly(
                new AppApiEndpointDTO(
                    "getContact", "GET", "contacts/{id}", Map.of("body", Map.of("type", "object")),
                    Map.of("success", Map.of("type", "string"))),
                new AppApiEndpointDTO("createContact", "POST", "contacts", null, null),
                new AppApiEndpointDTO("deleteContact", "DELETE", "contacts/{id}", null, null));

        AppApiCollectionDTO missingApiCollectionDTO = appApiCollectionDTOs.get(1);

        assertThat(missingApiCollectionDTO)
            .isEqualTo(new AppApiCollectionDTO(99L, "#99", null, 0, false, List.of()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRecordApiCallRecordsWorkflowChosenErrorStatusAsError() {
        when(toolExecutionRecorder.recordWithErrorMessage(any(ToolExecutionEvent.Builder.class), any(Supplier.class),
            any(Function.class)))
                .thenAnswer(invocation -> ((Supplier<Object>) invocation.getArgument(1)).get());

        Integer status = appApiExecutionFacade.recordApiCall("crm", "getContact", () -> 503, Integer::intValue);

        assertThat(status).isEqualTo(503);

        ArgumentCaptor<Function<Object, String>> errorMessageOfArgumentCaptor = ArgumentCaptor.forClass(Function.class);

        verify(toolExecutionRecorder).recordWithErrorMessage(
            any(ToolExecutionEvent.Builder.class), any(Supplier.class), errorMessageOfArgumentCaptor.capture());

        Function<Object, String> errorMessageOf = errorMessageOfArgumentCaptor.getValue();

        assertThat(errorMessageOf.apply(503)).isEqualTo("HTTP 503");
        assertThat(errorMessageOf.apply(404)).isEqualTo("HTTP 404");
        assertThat(errorMessageOf.apply(400)).isEqualTo("HTTP 400");
        assertThat(errorMessageOf.apply(399)).isNull();
        assertThat(errorMessageOf.apply(201)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRecordApiCallRecordsTheCallingApp() {
        when(toolExecutionRecorder.recordWithErrorMessage(any(ToolExecutionEvent.Builder.class), any(Supplier.class),
            any(Function.class)))
                .thenAnswer(invocation -> ((Supplier<Object>) invocation.getArgument(1)).get());

        String result = appApiExecutionFacade.recordApiCall("crm", "getContact", () -> "called", called -> 200);

        assertThat(result).isEqualTo("called");

        ArgumentCaptor<ToolExecutionEvent.Builder> builderArgumentCaptor =
            ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);

        verify(toolExecutionRecorder).recordWithErrorMessage(builderArgumentCaptor.capture(), any(Supplier.class),
            any(Function.class));

        ToolExecutionEvent toolExecutionEvent = builderArgumentCaptor.getValue()
            .build();

        assertThat(toolExecutionEvent.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_API);
        assertThat(toolExecutionEvent.kind()).isEqualTo(ToolExecutionKind.WORKFLOW);
        assertThat(toolExecutionEvent.toolName()).isEqualTo("getContact");
        assertThat(toolExecutionEvent.operationName()).isEqualTo("crm");
        assertThat(toolExecutionEvent.appId()).isEqualTo(APP_ID);
        assertThat(toolExecutionEvent.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(toolExecutionEvent.environment()).isEqualTo(Environment.DEVELOPMENT.ordinal());
        assertThat(toolExecutionEvent.type()).isEqualTo(PlatformType.AUTOMATION);
    }

    private static ApiCollection apiCollection(long id, String contextPath) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(1);
        apiCollection.setContextPath(contextPath);
        apiCollection.setId(id);
        apiCollection.setName(contextPath);

        return apiCollection;
    }

    private static App app(AppStatus status) {
        App app = new App();

        app.setEnvironment(Environment.DEVELOPMENT);
        app.setId(APP_ID);
        app.setStatus(status);
        app.setWorkspaceId(WORKSPACE_ID);

        return app;
    }

    private static AppApiCollection binding(long apiCollectionId) {
        AppApiCollection appApiCollection = new AppApiCollection();

        appApiCollection.setApiCollectionId(apiCollectionId);
        appApiCollection.setAppId(APP_ID);

        return appApiCollection;
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

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static Workflow workflow(Map<String, ?> triggerParameters) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), any()))
            .thenReturn(
                List.of(
                    new WorkflowTrigger(
                        Map.of(
                            "name", "trigger_1", "type", "apiPlatform/v1/newApiRequest", "parameters",
                            triggerParameters))));

        return workflow;
    }
}

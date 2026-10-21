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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.BindableWorkflow;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.CallableWorkflow;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppWorkflowFacadeTest {

    private static final String FIRST_UUID = "0b0f3a3e-0000-4000-8000-000000000001";
    private static final String SECOND_UUID = "0b0f3a3e-0000-4000-8000-000000000002";

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppCallableWorkflowResolver appCallableWorkflowResolver;

    @Mock
    private AppService appService;

    @Mock
    private AppWorkflowService appWorkflowService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ResourceVisibilityResolver resourceVisibilityResolver;

    private App app;
    private AppWorkflowFacadeImpl appWorkflowFacade;

    @BeforeEach
    void beforeEach() {
        app = new App();

        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setEnvironment(Environment.PRODUCTION);

        appWorkflowFacade = new AppWorkflowFacadeImpl(
            appAuditPublisher, appCallableWorkflowResolver, appService, appWorkflowService, projectDeploymentService,
            projectService, resourceVisibilityResolver);
    }

    @Test
    void testGetWorkflowBindingsMarksUnresolvableBindingsUnavailable() {
        AppWorkflow resolvable = appWorkflow(5L, "approve_refund", 11L, FIRST_UUID);
        AppWorkflow unresolvable = appWorkflow(6L, "cancel_order", 12L, SECOND_UUID);

        when(appService.getApp(1L)).thenReturn(app);
        when(appWorkflowService.getBindings(1L)).thenReturn(List.of(resolvable, unresolvable));
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, FIRST_UUID))
            .thenReturn(Optional.of(callableWorkflow("Approve refund", null)));
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 12L, SECOND_UUID))
            .thenReturn(Optional.empty());
        when(projectDeploymentService.fetchProjectDeployment(11L))
            .thenReturn(Optional.of(projectDeployment(11L, 3L, "Refunds")));
        when(projectDeploymentService.fetchProjectDeployment(12L)).thenReturn(Optional.empty());

        List<AppWorkflowBindingDTO> bindings = appWorkflowFacade.getWorkflowBindings(1L);

        assertThat(bindings).containsExactly(
            new AppWorkflowBindingDTO(5L, 1L, "approve_refund", 11L, FIRST_UUID, "Refunds", "Approve refund", true),
            new AppWorkflowBindingDTO(6L, 1L, "cancel_order", 12L, SECOND_UUID, null, null, false));
    }

    @Test
    void testGetBindableWorkflowsLeavesOutProjectsTheCallerCannotSee() {
        Project visibleProject = project(3L, "Refund project", ResourceVisibility.WORKSPACE, "alice");
        Project hiddenProject = project(4L, "Secret project", ResourceVisibility.PRIVATE, "bob");

        BindableWorkflow visibleWorkflow = new BindableWorkflow(
            projectDeployment(11L, 3L, "Refunds"), visibleProject, FIRST_UUID,
            callableWorkflow("Approve refund", "Approves a refund"));
        BindableWorkflow hiddenWorkflow = new BindableWorkflow(
            projectDeployment(12L, 4L, "Secrets"), hiddenProject, SECOND_UUID, callableWorkflow("Leak", null));

        when(appService.getApp(1L)).thenReturn(app);
        when(appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.PRODUCTION))
            .thenReturn(List.of(visibleWorkflow, hiddenWorkflow));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of(3L));

        List<AppBindableWorkflowDTO> bindableWorkflows = appWorkflowFacade.getBindableWorkflows(1L);

        assertThat(bindableWorkflows).containsExactly(
            new AppBindableWorkflowDTO(
                11L, "Refunds", "Refund project", FIRST_UUID, "Approve refund", "Approves a refund"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<VisibilityRecord>> recordsCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(resourceVisibilityResolver).filterVisibleIds(eq("Project"), eq(7L), recordsCaptor.capture());

        assertThat(recordsCaptor.getValue()).containsExactly(
            new VisibilityRecord(3L, ResourceVisibility.WORKSPACE, "alice"),
            new VisibilityRecord(4L, ResourceVisibility.PRIVATE, "bob"));
    }

    @Test
    void testGetBindableWorkflowsDoesNotAskTheResolverWhenThereAreNone() {
        when(appService.getApp(1L)).thenReturn(app);
        when(appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.PRODUCTION)).thenReturn(List.of());

        assertThat(appWorkflowFacade.getBindableWorkflows(1L)).isEmpty();

        verifyNoInteractions(resourceVisibilityResolver);
    }

    @Test
    void testBindWorkflowAuditsANewBinding() {
        AppWorkflow bound = appWorkflow(5L, "approve_refund", 11L, FIRST_UUID);

        stubVisibleProject(11L, 3L);

        when(appService.getApp(1L)).thenReturn(app);
        when(appWorkflowService.fetchBinding(1L, "approve_refund")).thenReturn(Optional.empty());
        when(appWorkflowService.bind(1L, "approve_refund", 11L, FIRST_UUID)).thenReturn(bound);
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, FIRST_UUID))
            .thenReturn(Optional.of(callableWorkflow("Approve refund", null)));

        AppWorkflowBindingDTO binding = appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, FIRST_UUID);

        assertThat(binding).isEqualTo(
            new AppWorkflowBindingDTO(5L, 1L, "approve_refund", 11L, FIRST_UUID, "Refunds", "Approve refund", true));

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_WORKFLOW_BOUND, 1L,
            Map.of("workflowKey", "approve_refund", "projectDeploymentId", 11L, "workflowUuid", FIRST_UUID));
    }

    @Test
    void testRebindingToAnotherWorkflowAuditsThePreviousOne() {
        AppWorkflow previous = appWorkflow(5L, "approve_refund", 12L, SECOND_UUID);
        AppWorkflow rebound = appWorkflow(5L, "approve_refund", 11L, FIRST_UUID);

        stubVisibleProject(11L, 3L);

        when(appService.getApp(1L)).thenReturn(app);
        when(appWorkflowService.fetchBinding(1L, "approve_refund")).thenReturn(Optional.of(previous));
        when(appWorkflowService.bind(1L, "approve_refund", 11L, FIRST_UUID)).thenReturn(rebound);
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, FIRST_UUID))
            .thenReturn(Optional.empty());

        appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, FIRST_UUID);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_WORKFLOW_BOUND, 1L,
            Map.of(
                "workflowKey", "approve_refund", "projectDeploymentId", 11L, "workflowUuid", FIRST_UUID,
                "previousProjectDeploymentId", 12L, "previousWorkflowUuid", SECOND_UUID));
    }

    @Test
    void testRebindingToTheSameWorkflowDoesNotReportAPreviousOne() {
        AppWorkflow previous = appWorkflow(5L, "approve_refund", 11L, FIRST_UUID);

        stubVisibleProject(11L, 3L);

        when(appService.getApp(1L)).thenReturn(app);
        when(appWorkflowService.fetchBinding(1L, "approve_refund")).thenReturn(Optional.of(previous));
        when(appWorkflowService.bind(1L, "approve_refund", 11L, FIRST_UUID)).thenReturn(previous);
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, FIRST_UUID))
            .thenReturn(Optional.empty());

        appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, FIRST_UUID);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_WORKFLOW_BOUND, 1L,
            Map.of("workflowKey", "approve_refund", "projectDeploymentId", 11L, "workflowUuid", FIRST_UUID));
    }

    @Test
    void testBindWorkflowRefusesAProjectTheCallerCannotSee() {
        Project project = project(3L, "Secret project", ResourceVisibility.PRIVATE, "bob");

        when(appService.getApp(1L)).thenReturn(app);
        when(projectDeploymentService.fetchProjectDeployment(11L))
            .thenReturn(Optional.of(projectDeployment(11L, 3L, "Refunds")));
        when(projectService.fetchProject(3L)).thenReturn(Optional.of(project));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of());

        assertConfigurationError(
            () -> appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, FIRST_UUID),
            AppErrorType.APP_WORKFLOW_NOT_BINDABLE);

        verify(appWorkflowService, never()).bind(anyLong(), anyString(), anyLong(), anyString());
        verify(appAuditPublisher, never()).publish(any(AppAuditEvent.class), anyLong(), anyMap());
    }

    @Test
    void testBindWorkflowRefusesAMissingDeployment() {
        when(appService.getApp(1L)).thenReturn(app);
        when(projectDeploymentService.fetchProjectDeployment(11L)).thenReturn(Optional.empty());

        assertConfigurationError(
            () -> appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, FIRST_UUID),
            AppErrorType.APP_WORKFLOW_NOT_BINDABLE);

        verify(appWorkflowService, never()).bind(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void testUnbindAuditsOnlyWhenABindingWasDeleted() {
        when(appWorkflowService.unbind(1L, "approve_refund")).thenReturn(true);
        when(appWorkflowService.unbind(1L, "missing")).thenReturn(false);

        appWorkflowFacade.unbindWorkflow(1L, "missing");

        verify(appAuditPublisher, never()).publish(any(AppAuditEvent.class), anyLong(), anyMap());

        appWorkflowFacade.unbindWorkflow(1L, "approve_refund");

        verify(appAuditPublisher).publish(AppAuditEvent.APP_WORKFLOW_UNBOUND, 1L,
            Map.of("workflowKey", "approve_refund"));
    }

    @Test
    void testGetCallableWorkflowsListsDevelopmentWorkflowsOfVisibleProjects() {
        Project visibleProject = project(3L, "Refund project", ResourceVisibility.WORKSPACE, "alice");
        Project hiddenProject = project(4L, "Secret project", ResourceVisibility.PRIVATE, "bob");

        when(appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.DEVELOPMENT)).thenReturn(List.of(
            new BindableWorkflow(
                projectDeployment(11L, 3L, "Refunds"), visibleProject, FIRST_UUID,
                new CallableWorkflow("wf-2", "trigger_1", "Approve refund", null, "{}", null, Map.of())),
            new BindableWorkflow(
                projectDeployment(12L, 4L, "Secrets"), hiddenProject, SECOND_UUID, callableWorkflow("Leak", null))));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of(3L));

        assertThat(appWorkflowFacade.getCallableWorkflows(7L, null)).containsExactly(
            new AppCallableWorkflowDTO(11L, FIRST_UUID, "Approve refund", 3L, "Refund project", "{}"));
    }

    @Test
    void testGetCallableWorkflowsKeepsOnlyTheGivenProject() {
        Project refunds = project(3L, "Refund project", ResourceVisibility.WORKSPACE, "alice");
        Project orders = project(4L, "Order project", ResourceVisibility.WORKSPACE, "alice");

        when(appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.DEVELOPMENT)).thenReturn(List.of(
            new BindableWorkflow(
                projectDeployment(11L, 3L, "Refunds"), refunds, FIRST_UUID, callableWorkflow("Refund", null)),
            new BindableWorkflow(
                projectDeployment(12L, 4L, "Orders"), orders, SECOND_UUID, callableWorkflow("Order", null))));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of(3L, 4L));

        assertThat(appWorkflowFacade.getCallableWorkflows(7L, 4L))
            .extracting(AppCallableWorkflowDTO::workflowUuid)
            .containsExactly(SECOND_UUID);
    }

    @Test
    void testEveryFacadeMethodIsGated() throws Exception {
        Map<String, String> expectedExpressions = Map.of(
            "getWorkflowBindings", "hasPermission(#appId, 'App', 'APP_VIEW')",
            "getBindableWorkflows", "hasPermission(#appId, 'App', 'APP_MANAGE')",
            "bindWorkflow", "hasPermission(#appId, 'App', 'APP_MANAGE')",
            "unbindWorkflow", "hasPermission(#appId, 'App', 'APP_MANAGE')",
            "getCallableWorkflows",
            "hasWorkspaceScopeInEnvironment(#workspaceId, 'DEPLOYMENT_VIEW', " +
                "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)");

        Method[] facadeMethods = AppWorkflowFacade.class.getDeclaredMethods();

        assertThat(facadeMethods).hasSameSizeAs(expectedExpressions.entrySet());

        for (Method facadeMethod : facadeMethods) {
            Method implementationMethod = AppWorkflowFacadeImpl.class.getMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = Objects.requireNonNull(
                implementationMethod.getAnnotation(PreAuthorize.class), facadeMethod.getName());

            assertThat(preAuthorize.value()).isEqualTo(expectedExpressions.get(facadeMethod.getName()));
        }
    }

    private void stubVisibleProject(long projectDeploymentId, long projectId) {
        ProjectDeployment projectDeployment = projectDeployment(projectDeploymentId, projectId, "Refunds");
        Project project = project(projectId, "Refund project", ResourceVisibility.WORKSPACE, "alice");

        when(projectDeploymentService.fetchProjectDeployment(projectDeploymentId))
            .thenReturn(Optional.of(projectDeployment));
        when(projectService.fetchProject(projectId)).thenReturn(Optional.of(project));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of(projectId));
    }

    private static AppWorkflow appWorkflow(long id, String workflowKey, long projectDeploymentId, String workflowUuid) {
        AppWorkflow appWorkflow = new AppWorkflow();

        appWorkflow.setAppId(1L);
        appWorkflow.setId(id);
        appWorkflow.setProjectDeploymentId(projectDeploymentId);
        appWorkflow.setWorkflowKey(workflowKey);
        appWorkflow.setWorkflowUuid(workflowUuid);

        return appWorkflow;
    }

    private static CallableWorkflow callableWorkflow(String label, String description) {
        return new CallableWorkflow("wf-2", "trigger_1", label, description, null, null, Map.of());
    }

    private static Project project(long id, String name, ResourceVisibility visibility, String createdBy) {
        Project project = mock(Project.class);

        lenient().when(project.getId())
            .thenReturn(id);
        lenient().when(project.getName())
            .thenReturn(name);
        lenient().when(project.getVisibility())
            .thenReturn(visibility);
        lenient().when(project.getCreatedBy())
            .thenReturn(createdBy);

        return project;
    }

    private static ProjectDeployment projectDeployment(long id, long projectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }

    private static void assertConfigurationError(
        ThrowingCallable callable, AppErrorType errorType) {

        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(errorType.getErrorKey());
    }
}

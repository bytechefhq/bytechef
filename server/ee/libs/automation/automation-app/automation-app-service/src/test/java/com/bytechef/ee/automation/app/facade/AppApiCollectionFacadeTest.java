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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppApiCollectionService.BindResult;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.workflow.AppApiCollectionResolver;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppApiCollectionFacadeTest {

    private static final long APP_ID = 1L;
    private static final long WORKSPACE_ID = 7L;

    @Mock
    private ApiCollectionService apiCollectionService;

    @Mock
    private AppApiCollectionResolver appApiCollectionResolver;

    @Mock
    private AppApiCollectionService appApiCollectionService;

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppService appService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ResourceVisibilityResolver resourceVisibilityResolver;

    private AppApiCollectionFacadeImpl appApiCollectionFacade;

    @BeforeEach
    void beforeEach() {
        App app = new App();

        app.setId(APP_ID);
        app.setWorkspaceId(WORKSPACE_ID);
        app.setEnvironment(Environment.PRODUCTION);

        lenient().when(appService.getApp(APP_ID))
            .thenReturn(app);

        appApiCollectionFacade = new AppApiCollectionFacadeImpl(
            apiCollectionService, appApiCollectionResolver, appApiCollectionService, appAuditPublisher, appService,
            projectDeploymentService, projectService, resourceVisibilityResolver);
    }

    @Test
    void testBindPublishesAudit() {
        ApiCollection apiCollection = apiCollection(21L, 11L);

        stubDeployment(11L, 3L, true);
        stubVisibleProjects(3L);

        when(appApiCollectionResolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(apiCollection));
        when(appApiCollectionService.bind(APP_ID, 21L)).thenReturn(new BindResult(binding(5L, 21L), true));
        when(apiCollectionService.fetchApiCollection(21L)).thenReturn(Optional.of(apiCollection));
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, 21L))
            .thenReturn(Optional.of(apiCollection));

        AppApiCollectionBindingDTO binding = appApiCollectionFacade.bindApiCollection(APP_ID, 21L);

        assertThat(binding).isEqualTo(
            new AppApiCollectionBindingDTO(5L, APP_ID, 21L, "CRM 21", "crm-21", 2, 3L, "Project 3", true));

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_API_COLLECTION_BOUND, APP_ID, Map.of("apiCollectionId", 21L, "name", "CRM 21"));
    }

    @Test
    void testRebindDoesNotPublishAudit() {
        ApiCollection apiCollection = apiCollection(21L, 11L);

        stubDeployment(11L, 3L, true);
        stubVisibleProjects(3L);

        when(appApiCollectionResolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(apiCollection));
        when(appApiCollectionService.bind(APP_ID, 21L)).thenReturn(new BindResult(binding(5L, 21L), false));
        when(apiCollectionService.fetchApiCollection(21L)).thenReturn(Optional.of(apiCollection));
        when(appApiCollectionResolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, 21L))
            .thenReturn(Optional.of(apiCollection));

        AppApiCollectionBindingDTO binding = appApiCollectionFacade.bindApiCollection(APP_ID, 21L);

        assertThat(binding.id()).isEqualTo(5L);

        verify(appAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testBindRefusesCollectionNotBindable() {
        when(appApiCollectionResolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(apiCollection(22L, 12L)));

        assertNotBindable(21L);
    }

    @Test
    void testBindRefusesInvisibleProject() {
        stubDeployment(11L, 3L, true);
        stubVisibleProjects();

        when(appApiCollectionResolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(apiCollection(21L, 11L)));

        assertNotBindable(21L);
    }

    @Test
    void testUnbindPublishesOnlyWhenDeleted() {
        when(appApiCollectionService.unbind(APP_ID, 21L)).thenReturn(true);
        when(appApiCollectionService.unbind(APP_ID, 22L)).thenReturn(false);

        appApiCollectionFacade.unbindApiCollection(APP_ID, 21L);
        appApiCollectionFacade.unbindApiCollection(APP_ID, 22L);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_API_COLLECTION_UNBOUND, APP_ID, Map.of("apiCollectionId", 21L));
        verify(appAuditPublisher, never()).publish(
            eq(AppAuditEvent.APP_API_COLLECTION_UNBOUND), eq(APP_ID), eq(Map.of("apiCollectionId", 22L)));
    }

    @Test
    void testBindingsReportUnavailable() {
        ApiCollection disabledApiCollection = apiCollection(21L, 11L);

        stubDeployment(11L, 3L, false);

        when(appApiCollectionService.getBindings(APP_ID)).thenReturn(List.of(binding(5L, 21L), binding(6L, 22L)));
        when(apiCollectionService.fetchApiCollection(21L)).thenReturn(Optional.of(disabledApiCollection));
        when(apiCollectionService.fetchApiCollection(22L)).thenReturn(Optional.empty());
        when(appApiCollectionResolver.resolve(eq(WORKSPACE_ID), eq(Environment.PRODUCTION), anyLong()))
            .thenReturn(Optional.empty());

        assertThat(appApiCollectionFacade.getApiCollectionBindings(APP_ID)).containsExactly(
            new AppApiCollectionBindingDTO(5L, APP_ID, 21L, "CRM 21", "crm-21", 2, 3L, "Project 3", false),
            new AppApiCollectionBindingDTO(6L, APP_ID, 22L, "#22", null, 0, 0L, null, false));
    }

    @Test
    void testBindableApiCollectionsLeaveOutInvisibleProjects() {
        ApiCollection visibleApiCollection = apiCollection(21L, 11L);
        ApiCollection hiddenApiCollection = apiCollection(22L, 12L);

        stubDeployment(11L, 3L, false);
        stubDeployment(12L, 4L, true);
        stubVisibleProjects(3L);

        when(appApiCollectionResolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(visibleApiCollection, hiddenApiCollection));

        assertThat(appApiCollectionFacade.getBindableApiCollections(APP_ID)).containsExactly(
            new AppBindableApiCollectionDTO(21L, "CRM 21", "crm-21", 2, 3L, "Project 3", false));
    }

    @Test
    void testEveryFacadeMethodIsGated() throws Exception {
        Map<String, String> expectedExpressions = Map.of(
            "getApiCollectionBindings", "hasPermission(#appId, 'App', 'APP_VIEW')",
            "getBindableApiCollections", "hasPermission(#appId, 'App', 'APP_MANAGE')",
            "bindApiCollection", "hasPermission(#appId, 'App', 'APP_MANAGE')",
            "unbindApiCollection", "hasPermission(#appId, 'App', 'APP_MANAGE')");

        Method[] facadeMethods = AppApiCollectionFacade.class.getDeclaredMethods();

        assertThat(facadeMethods).hasSameSizeAs(expectedExpressions.entrySet());

        for (Method facadeMethod : facadeMethods) {
            Method implementationMethod = AppApiCollectionFacadeImpl.class.getMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = Objects.requireNonNull(
                implementationMethod.getAnnotation(PreAuthorize.class), facadeMethod.getName());

            assertThat(preAuthorize.value()).isEqualTo(expectedExpressions.get(facadeMethod.getName()));
        }
    }

    private void assertNotBindable(long apiCollectionId) {
        assertThatThrownBy(() -> appApiCollectionFacade.bindApiCollection(APP_ID, apiCollectionId))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_API_COLLECTION_NOT_BINDABLE.getErrorKey());

        verify(appApiCollectionService, never()).bind(anyLong(), anyLong());
        verify(appAuditPublisher, never()).publish(any(AppAuditEvent.class), anyLong(), anyMap());
    }

    private void stubDeployment(long projectDeploymentId, long projectId, boolean enabled) {
        Project project = project(projectId);
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(enabled);
        projectDeployment.setId(projectDeploymentId);
        projectDeployment.setProjectId(projectId);

        lenient().when(projectDeploymentService.fetchProjectDeployment(projectDeploymentId))
            .thenReturn(Optional.of(projectDeployment));
        lenient().when(projectService.fetchProject(projectId))
            .thenReturn(Optional.of(project));
    }

    private void stubVisibleProjects(Long... projectIds) {
        when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(WORKSPACE_ID), any()))
            .thenReturn(Set.of(projectIds));
    }

    private static ApiCollection apiCollection(long id, long projectDeploymentId) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(2);
        apiCollection.setContextPath("crm-" + id);
        apiCollection.setId(id);
        apiCollection.setName("CRM " + id);
        apiCollection.setProjectDeploymentId(projectDeploymentId);

        return apiCollection;
    }

    private static AppApiCollection binding(long id, long apiCollectionId) {
        AppApiCollection appApiCollection = new AppApiCollection();

        appApiCollection.setApiCollectionId(apiCollectionId);
        appApiCollection.setAppId(APP_ID);
        appApiCollection.setId(id);

        return appApiCollection;
    }

    private static Project project(long id) {
        Project project = mock(Project.class);

        lenient().when(project.getId())
            .thenReturn(id);
        lenient().when(project.getName())
            .thenReturn("Project " + id);
        lenient().when(project.getVisibility())
            .thenReturn(ResourceVisibility.WORKSPACE);
        lenient().when(project.getCreatedBy())
            .thenReturn("alice");

        return project;
    }
}

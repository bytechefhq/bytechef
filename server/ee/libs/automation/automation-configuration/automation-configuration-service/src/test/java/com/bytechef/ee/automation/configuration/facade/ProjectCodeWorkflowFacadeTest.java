/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeTest {

    private static final String BODY_REACHED = "body reached";
    private static final String PROJECT_NAME = "orders";
    private static final long WORKSPACE_ID = 42L;
    private static final String RESOURCE_TYPE = "Workspace";
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
    private final ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private final ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
        mock(CacheManager.class), projectService, projectWorkflowService, codeWorkflowContainerFacade,
        projectCodeWorkflowService);

    @Test
    void testSaveCreatesTheProjectInTheWorkspaceWhenTheWorkspaceHasNoProjectOfThatName() {
        Project createdProject = new Project();

        createdProject.setId(7L);

        when(projectService.fetchProject(PROJECT_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(projectService.create(any(Project.class))).thenReturn(createdProject);

        save();

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).create(projectArgumentCaptor.capture());

        Project project = projectArgumentCaptor.getValue();

        assertThat(project.getWorkspaceId()).isEqualTo(WORKSPACE_ID);

        verify(projectService, never()).fetchProject(PROJECT_NAME);
        verify(projectService, never()).update(any(Project.class));
        verify(projectService).publishProject(7L, null, false);
    }

    @Test
    void testSaveUpdatesTheProjectOfThatNameInTheSameWorkspace() {
        Project existingProject = new Project();

        existingProject.setId(9L);
        existingProject.setWorkspaceId(WORKSPACE_ID);

        when(projectService.fetchProject(PROJECT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(existingProject));
        when(projectService.update(existingProject)).thenReturn(existingProject);

        save();

        verify(projectService).update(existingProject);
        verify(projectService, never()).create(any(Project.class));
        verify(projectService, never()).fetchProject(PROJECT_NAME);
        verify(projectService).publishProject(9L, null, false);
    }

    private void save() {
        ProjectDefinition projectDefinition = mock(ProjectDefinition.class);

        when(projectDefinition.getName()).thenReturn(PROJECT_NAME);
        when(projectDefinition.getDescription()).thenReturn(Optional.empty());

        ProjectHandler projectHandler = mock(ProjectHandler.class);

        when(projectHandler.getDefinition()).thenReturn(projectDefinition);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getWorkflowNameIds()).thenReturn(Map.of());
        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any()))
            .thenReturn(codeWorkflowContainer);

        try (MockedStatic<ProjectHandlerLoader> projectHandlerLoaderMockedStatic =
            mockStatic(ProjectHandlerLoader.class)) {

            projectHandlerLoaderMockedStatic
                .when(() -> ProjectHandlerLoader.loadProjectHandler(any(), any(), anyString(), any()))
                .thenReturn(projectHandler);

            projectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA);
        }
    }

    @Test
    void testSaveAllowsWhenBothProjectCreateAndPublishScopesAreGranted() {
        assertSaveGuard(true, true);
    }

    @Test
    void testSaveDeniesWhenTheProjectCreateScopeIsRefused() {
        assertSaveGuard(false, true);
    }

    @Test
    void testSaveDeniesWhenTheProjectPublishScopeIsRefused() {
        assertSaveGuard(true, false);
    }

    private void assertSaveGuard(boolean projectCreateGranted, boolean projectPublishGranted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(WORKSPACE_ID, RESOURCE_TYPE, "PROJECT_CREATE"))
            .thenReturn(projectCreateGranted);
        when(permissionService.hasResourceScope(WORKSPACE_ID, RESOURCE_TYPE, "PROJECT_PUBLISH"))
            .thenReturn(projectPublishGranted);

        ProjectCodeWorkflowFacade securedProjectCodeWorkflowFacade = secure(permissionService);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);

        try (MockedStatic<ProjectHandlerLoader> projectHandlerLoaderMockedStatic =
            mockStatic(ProjectHandlerLoader.class)) {

            projectHandlerLoaderMockedStatic
                .when(() -> ProjectHandlerLoader.loadProjectHandler(any(), any(), anyString(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            if (projectCreateGranted && projectPublishGranted) {
                assertThatThrownBy(
                    () -> securedProjectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(
                    () -> securedProjectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA))
                        .isInstanceOf(AccessDeniedException.class);

                projectHandlerLoaderMockedStatic.verifyNoInteractions();
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private ProjectCodeWorkflowFacade secure(PermissionService permissionService) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(projectCodeWorkflowFacade);

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (ProjectCodeWorkflowFacade) proxyFactory.getProxy();
    }
}

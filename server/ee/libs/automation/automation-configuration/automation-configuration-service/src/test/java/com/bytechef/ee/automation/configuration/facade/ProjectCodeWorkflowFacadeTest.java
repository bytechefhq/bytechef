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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
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

    private final ProjectCodeWorkflowDraftFacade projectCodeWorkflowDraftFacade =
        mock(ProjectCodeWorkflowDraftFacade.class);
    private final ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final PermissionService projectPermissionService = mock(PermissionService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
        createApplicationProperties(), mock(CacheManager.class), projectService, mock(ProjectWorkflowService.class),
        projectFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
        mock(CodeWorkflowFileStorage.class), projectCodeWorkflowDraftFacade, mock(TagService.class),
        mock(ProjectDeploymentFacade.class), mock(ProjectDeploymentService.class),
        mock(ProjectDeploymentWorkflowService.class), mock(ComponentConnectionFacade.class),
        mock(WorkflowService.class), mock(ProjectOwnershipService.class), projectPermissionService);

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

        verify(projectService, never()).update(any(Project.class));
        verify(projectFacade).publishProject(7L, null, false);
    }

    @Test
    void testSaveUpdatesTheProjectOfThatNameInTheSameWorkspace() {
        Project existingProject = new Project();

        existingProject.setId(9L);
        existingProject.setWorkspaceId(WORKSPACE_ID);

        when(projectService.fetchProject(PROJECT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(existingProject));
        when(projectService.update(existingProject)).thenReturn(existingProject);
        when(projectPermissionService.hasResourceScope(9L, "Project", "WORKFLOW_EDIT")).thenReturn(true);
        when(projectPermissionService.hasResourceScope(9L, "Project", "PROJECT_PUBLISH")).thenReturn(true);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(9L))
            .thenReturn(Optional.of(mock(ProjectCodeWorkflow.class)));

        save();

        verify(projectService).update(existingProject);
        verify(projectService, never()).create(any(Project.class));
        verify(projectFacade).publishProject(9L, null, false);
    }

    private void save() {
        ProjectDefinition projectDefinition = mock(ProjectDefinition.class);

        when(projectDefinition.getName()).thenReturn(PROJECT_NAME);
        when(projectDefinition.getDescription()).thenReturn(Optional.empty());

        ProjectHandler projectHandler = mock(ProjectHandler.class);

        when(projectHandler.getDefinition()).thenReturn(projectDefinition);

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
                        .isInstanceOf(ConfigurationException.class)
                        .hasMessageContaining(BODY_REACHED);
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
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        ObjectProvider<MethodSecurityExpressionContributor> contributorProvider =
            beanFactory.getBeanProvider(MethodSecurityExpressionContributor.class);

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService, contributorProvider);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(projectCodeWorkflowFacade);

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (ProjectCodeWorkflowFacade) proxyFactory.getProxy();
    }

    private static ApplicationProperties createApplicationProperties() {
        ApplicationProperties.Workflow workflow = new ApplicationProperties.Workflow();

        ApplicationProperties.Workflow.CodeWorkflow codeWorkflow = workflow.getCodeWorkflow();

        ApplicationProperties.Workflow.CodeWorkflow.Java java = codeWorkflow.getJava();

        java.setEnabled(true);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWorkflow()).thenReturn(workflow);

        return applicationProperties;
    }
}

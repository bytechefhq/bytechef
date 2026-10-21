/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectCodeWorkflowFacadeIntTest.Config.class, properties = "bytechef.edition=ee")
@MockitoBean(types = {
    ApplicationProperties.class, CacheManager.class, CategoryService.class, CodeWorkflowContainerFacade.class,
    ComponentConnectionFacade.class, ComponentDefinitionHelper.class, ConnectionFacade.class,
    ConnectionLifecycleFacade.class, ConnectionService.class, EnvironmentService.class, Evaluator.class,
    JobFacade.class, JobService.class, PreBuiltTemplateService.class, PrincipalJobFacade.class,
    PrincipalJobService.class, ProjectCodeWorkflowService.class, ProjectDeploymentService.class,
    ProjectDeploymentWorkflowService.class, ProjectService.class, ProjectWorkflowService.class,
    SharedTemplateFileStorage.class, SharedTemplateService.class, TagService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, WorkflowCacheManager.class, WorkflowFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowService.class, WorkflowTestConfigurationService.class,
    WorkflowValidatorFacade.class, WorkspaceConnectionService.class, WorkspaceService.class
})
class ProjectCodeWorkflowFacadeIntTest {

    private static final long WORKSPACE_ID = 21L;

    @Autowired
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade;

    @Autowired
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Autowired
    private ProjectService projectService;

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        authenticate(AuthorityConstants.USER);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveIsAdminOnly() {
        assertRequiresAdminAuthority(() -> projectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA));
    }

    @Test
    void testSaveUpdatesTheSameNamedProjectOfTheCallerWorkspace() {
        authenticate(AuthorityConstants.ADMIN);

        Project project = new Project();

        project.setId(5L);
        project.setName("Orders");
        project.setWorkspaceId(WORKSPACE_ID);

        when(projectService.fetchProject("Orders", WORKSPACE_ID)).thenReturn(Optional.of(project));
        when(projectService.update(project)).thenReturn(project);

        saveProjectNamed("Orders");

        verify(projectService).update(project);
        verify(projectService, never()).create(any());
        verify(projectCodeWorkflowService).create(any(), eq(project));
    }

    @Test
    void testSaveCreatesAProjectInTheCallerWorkspaceWhenItHasNoProjectOfThatName() {
        authenticate(AuthorityConstants.ADMIN);

        Project createdProject = new Project();

        createdProject.setId(6L);

        when(projectService.fetchProject("Orders", WORKSPACE_ID)).thenReturn(Optional.empty());
        when(projectService.create(any())).thenReturn(createdProject);

        saveProjectNamed("Orders");

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).create(projectArgumentCaptor.capture());
        verify(projectService, never()).update(any());

        Project project = projectArgumentCaptor.getValue();

        assertThat(project.getName()).isEqualTo("Orders");
        assertThat(project.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    }

    private void saveProjectNamed(String name) {
        ProjectDefinition projectDefinition = mock(ProjectDefinition.class);

        when(projectDefinition.getName()).thenReturn(name);

        ProjectHandler projectHandler = mock(ProjectHandler.class);

        when(projectHandler.getDefinition()).thenReturn(projectDefinition);
        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(CodeWorkflowContainer.class));

        try (MockedStatic<ProjectHandlerLoader> projectHandlerLoader = mockStatic(ProjectHandlerLoader.class)) {
            projectHandlerLoader
                .when(() -> ProjectHandlerLoader.loadProjectHandler(any(), any(), any(), any()))
                .thenReturn(projectHandler);

            projectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.JAVA);
        }
    }

    private void assertRequiresAdminAuthority(ThrowingCallable call) {
        authenticate(AuthorityConstants.USER);

        assertDenied(call);

        authenticate(AuthorityConstants.ADMIN);

        assertNotDenied(call);

        authenticate(AuthorityConstants.USER);
    }

    private static void assertDenied(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    private static void assertNotDenied(ThrowingCallable call) {
        Throwable throwable = catchThrowable(call);

        assertThat(throwable)
            .as("Expected the gate to let the call through, but it was denied: %s", throwable)
            .satisfiesAnyOf(
                actual -> assertThat(actual).isNull(),
                actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ProjectCodeWorkflowFacadeImpl.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}

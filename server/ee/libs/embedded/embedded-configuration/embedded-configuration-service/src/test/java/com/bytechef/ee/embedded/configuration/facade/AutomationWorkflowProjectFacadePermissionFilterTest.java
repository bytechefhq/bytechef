/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Focused unit tests for permission-expression persistence and connected-user project/workflow filtering in
 * {@link AutomationWorkflowProjectFacadeImpl}. Uses a real {@link EmbeddedPermissionEvaluator} (real SpEL) and mocked
 * collaborators.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectFacadePermissionFilterTest {

    private static final String MARKER = "__EMBEDDED_AUTOMATION__";

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator =
        new EmbeddedPermissionEvaluator(SpelEvaluator.create());
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TagService tagService = mock(TagService.class);
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService =
        mock(WorkflowNodeTestOutputService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    private final AutomationWorkflowProjectFacadeImpl facade = new AutomationWorkflowProjectFacadeImpl(
        categoryService, componentDefinitionService, connectedUserService, embeddedPermissionEvaluator,
        projectService, projectWorkflowFacade, projectWorkflowService, tagService, workflowNodeTestOutputService,
        workflowService, workflowTestConfigurationService);

    @Test
    void testGetPublishedProjectsHidesProjectWhenExpressionIsFalse() {
        ConnectedUser connectedUser = connectedUser(Map.of("plan", "free"));

        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);
        when(projectService.getProjects()).thenReturn(List.of(
            markedPublishedProject(1L, "Pro", "metadata['plan'] == 'pro'"),
            markedPublishedProject(2L, "Free", null)));

        List<AutomationWorkflowProjectDTO> projects = facade.getPublishedProjects("user-1", Environment.PRODUCTION);

        assertThat(projects)
            .extracting(AutomationWorkflowProjectDTO::name)
            .containsExactly("Free");
    }

    @Test
    void testUpdateProjectDoesNotClobberExpressionWhenArgumentIsNull() {
        Project project = markedPublishedProject(7L, "Pro", "metadata['plan'] == 'pro'");

        when(projectService.getProject(7L)).thenReturn(project);

        facade.updateProject(7L, "Pro", "desc", null, List.of(), null);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).update(captor.capture());

        assertThat(captor.getValue()
            .getPermissionExpression()).isEqualTo("metadata['plan'] == 'pro'");
    }

    @Test
    void testUpdateProjectClearsExpressionWhenArgumentIsBlank() {
        Project project = markedPublishedProject(7L, "Pro", "metadata['plan'] == 'pro'");

        when(projectService.getProject(7L)).thenReturn(project);

        facade.updateProject(7L, "Pro", "desc", null, List.of(), "");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).update(captor.capture());

        assertThat(captor.getValue()
            .getPermissionExpression()).isNull();
    }

    @Test
    void testUpdateProjectWorkflowPermissionExpressionWritesJoinEntity() {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(1L, 1, "wf-1");

        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1")).thenReturn(projectWorkflow);
        when(projectService.getProject(1L)).thenReturn(markedPublishedProject(1L, "P", null));

        facade.updateProjectWorkflowPermissionExpression("wf-1", "metadata['tier'] == 'gold'");

        assertThat(projectWorkflow.getPermissionExpression()).isEqualTo("metadata['tier'] == 'gold'");

        verify(projectWorkflowService).update(projectWorkflow);
    }

    private static ConnectedUser connectedUser(Map<String, String> metadata) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setExternalId("user-1");
        connectedUser.setEnvironment(Environment.PRODUCTION);
        connectedUser.setMetadata(metadata);

        return connectedUser;
    }

    private static Project markedPublishedProject(long id, String displayName, String permissionExpression) {
        Project project = new Project();

        project.setId(id);
        project.setName(MARKER + displayName);
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        project.setPermissionExpression(permissionExpression);

        return project;
    }
}

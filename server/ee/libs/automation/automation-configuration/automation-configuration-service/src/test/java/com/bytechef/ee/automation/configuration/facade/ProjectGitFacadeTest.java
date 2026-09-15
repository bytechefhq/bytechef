/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository.GitWorkflows;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.GitInfo;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * A pull writes every workflow on the branch and then publishes the project once, so one pull adds one project version.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectGitFacadeTest {

    private static final long PROJECT_ID = 42L;
    private static final long WORKSPACE_ID = 7L;
    private static final String RESOURCE_TYPE = "Project";
    private final GitConfigurationFacade gitConfigurationFacade = mock(GitConfigurationFacade.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);

    private final ProjectGitConfigurationService projectGitConfigurationService =
        mock(ProjectGitConfigurationService.class);

    private final ProjectGitService projectGitService = mock(ProjectGitService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkspaceService workspaceService = mock(WorkspaceService.class);
    private ProjectGitFacadeImpl projectGitFacade;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();

        workspace.setId(WORKSPACE_ID);

        ProjectGitConfiguration projectGitConfiguration = mock(ProjectGitConfiguration.class);

        when(projectGitConfiguration.getBranch()).thenReturn("main");

        when(workspaceService.getProjectWorkspace(PROJECT_ID)).thenReturn(workspace);
        when(gitConfigurationFacade.getGitConfiguration(WORKSPACE_ID))
            .thenReturn(new GitConfigurationDTO("https://example.com/repository.git", "user", "secret"));
        when(projectGitConfigurationService.getProjectGitConfiguration(PROJECT_ID))
            .thenReturn(projectGitConfiguration);
        when(projectService.getProject(PROJECT_ID)).thenReturn(mock(Project.class));
        when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt())).thenReturn(List.of());

        projectGitFacade = new ProjectGitFacadeImpl(
            gitConfigurationFacade, projectFacade, projectGitConfigurationService, projectGitService, projectService,
            projectWorkflowFacade, projectWorkflowService, workflowService, workspaceService);
    }

    @Test
    void testPullPublishesTheProjectOnceForAllWorkflows() {
        givenBranchWorkflows(List.of(workflow("First", "{}"), workflow("Second", "{}"), workflow("Third", "{}")));

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        verify(projectWorkflowFacade, times(3)).addWorkflow(PROJECT_ID, "{}");
        verify(projectFacade, times(1)).publishProject(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testPullOfAnEmptyBranchDoesNotPublish() {
        givenBranchWorkflows(List.of());

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        verify(projectFacade, never()).publishProject(anyLong(), anyString(), anyBoolean());
    }

    private void givenBranchWorkflows(List<Workflow> workflows) {
        when(projectGitService.getWorkflows("https://example.com/repository.git", "main", "user", "secret"))
            .thenReturn(new GitWorkflows(workflows, new GitInfo("abc123", "Update workflows")));
    }

    private static Workflow workflow(String label, String definition) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn(label);
        when(workflow.getDefinition()).thenReturn(definition);

        return workflow;
    }

    @Test
    void testPullProjectFromGitDeniesWhenTheProjectPullScopeIsRefused() throws Exception {
        assertGuard(
            pullProjectFromGitMethod(), new Object[] {
                PROJECT_ID
            }, "PROJECT_PULL", false);
    }

    @Test
    void testPullProjectFromGitAllowsWhenTheProjectPullScopeIsGranted() throws Exception {
        assertGuard(
            pullProjectFromGitMethod(), new Object[] {
                PROJECT_ID
            }, "PROJECT_PULL", true);
    }

    @Test
    void testPushProjectToGitDeniesWhenTheProjectPushScopeIsRefused() throws Exception {
        assertGuard(
            pushProjectToGitMethod(), new Object[] {
                PROJECT_ID, "Update workflows"
            }, "PROJECT_PUSH", false);
    }

    @Test
    void testPushProjectToGitAllowsWhenTheProjectPushScopeIsGranted() throws Exception {
        assertGuard(
            pushProjectToGitMethod(), new Object[] {
                PROJECT_ID, "Update workflows"
            }, "PROJECT_PUSH", true);
    }

    private void assertGuard(Method method, Object[] arguments, String expectedScope, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_ID, RESOURCE_TYPE, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", PROJECT_ID, RESOURCE_TYPE, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(PROJECT_ID, RESOURCE_TYPE, expectedScope);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode -- reading it rather than restating it as a literal is the entire point of the test, since a literal
    // could drift from the guard it claims to verify. It is not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, Object[] arguments) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method pullProjectFromGitMethod() throws NoSuchMethodException {
        return ProjectGitFacadeImpl.class.getMethod("pullProjectFromGit", long.class);
    }

    private static Method pushProjectToGitMethod() throws NoSuchMethodException {
        return ProjectGitFacadeImpl.class.getMethod("pushProjectToGit", long.class, String.class);
    }
}

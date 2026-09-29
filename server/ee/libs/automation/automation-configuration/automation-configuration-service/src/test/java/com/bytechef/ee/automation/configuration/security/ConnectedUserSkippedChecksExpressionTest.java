/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionRoot;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.util.ReflectionUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserSkippedChecksExpressionTest {

    private static final long PROJECT_ID = 9L;
    private static final Method TO_STRING_METHOD = ReflectionUtils.findMethod(Object.class, "toString");
    private static final long RESOURCE_ID = 5L;
    private static final String RESOURCE_TYPE = "ProjectDeployment";
    private static final String SCOPE = "DEPLOYMENT_EDIT";
    private static final String WORKFLOW_ID = "workflow-1";
    private static final long WORKSPACE_ID = 1L;

    private CurrentUserResolver currentUserResolver;
    private AutomationPermissionEvaluator evaluator;
    private ProjectRepository projectRepository;
    private AutomationMethodSecurityExpressionRoot root;
    private WorkspaceScopeCacheService workspaceScopeCacheService;

    @BeforeEach
    void setUp() {
        currentUserResolver = mock(CurrentUserResolver.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        ConnectedUserAccessDecider decider = mock(ConnectedUserAccessDecider.class);

        when(decider.decide(any(Serializable.class), anyString(), anyString())).thenReturn(Decision.DENY);
        when(decider.decideInEnvironment(any(Serializable.class), anyString(), anyString(), any()))
            .thenReturn(Decision.DENY);
        when(decider.decideWorkflow(anyString(), anyString())).thenReturn(Decision.DENY);
        when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.DENY);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of("connectedUserAccessDecider", decider));

        PermissionServiceImpl permissionService = new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository, workspaceScopeCacheService,
            mock(WorkspaceUserRepository.class), List.of(), List.of(),
            beanFactory.getBeanProvider(ConnectedUserAccessDecider.class));

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        MethodInvocation methodInvocation = mock(MethodInvocation.class);

        when(methodInvocation.getMethod()).thenReturn(TO_STRING_METHOD);

        EvaluationContext evaluationContext = expressionHandler.createEvaluationContext(
            () -> mock(Authentication.class), methodInvocation);

        root = (AutomationMethodSecurityExpressionRoot) evaluationContext.getRootObject()
            .getValue();

        evaluator = new AutomationPermissionEvaluator(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(
            () -> root.hasWorkspaceScopeInEnvironment(WORKSPACE_ID, SCOPE, Environment.PRODUCTION));
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 2L));
        assertDeniedUnderSkippedChecks(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, null));
        assertDeniedUnderSkippedChecks(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 99L));
    }

    @Test
    void testHasResourceScopeInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(
            () -> root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION));
        assertDeniedUnderSkippedChecks(
            () -> root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null));
    }

    @Test
    void testHasResourceScopeInEnvironmentIdIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(
            () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, 2L));
        assertDeniedUnderSkippedChecks(
            () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null));
        assertDeniedUnderSkippedChecks(
            () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, 99L));
    }

    @Test
    void testHasWorkflowScopeIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(() -> root.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_VIEW"));
    }

    @Test
    void testHasWorkflowScopeInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(
            () -> root.hasWorkflowScopeInEnvironment(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION));
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironment(
                WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT));
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", 0L));
        assertDeniedUnderSkippedChecks(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", null));
        assertDeniedUnderSkippedChecks(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", 99L));
    }

    @Test
    void testHasPermissionByIdIsDeniedUnderSkippedChecks() throws Throwable {
        assertDeniedUnderSkippedChecks(() -> evaluator.hasPermission(null, RESOURCE_ID, RESOURCE_TYPE, SCOPE));
    }

    @Test
    void testHasPermissionOnAProjectDeploymentIsDeniedUnderSkippedChecks() throws Throwable {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setProjectId(PROJECT_ID);

        ProjectDeploymentDTO projectDeploymentDTO = new ProjectDeploymentDTO(projectDeployment);

        assertDeniedUnderSkippedChecks(
            () -> evaluator.hasPermission(null, projectDeploymentDTO, "DEPLOYMENT_CREATE"));
    }

    private void assertDeniedUnderSkippedChecks(BooleanSupplier check) throws Throwable {
        assertThat(AutomationAuthorizationContext.callSkippingChecks(check::getAsBoolean)).isFalse();
        assertThat(check.getAsBoolean()).isFalse();

        verifyNoInteractions(currentUserResolver, projectRepository, workspaceScopeCacheService);
    }
}

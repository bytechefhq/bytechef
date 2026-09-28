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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security.ConnectedUserAuthorizationMode;
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
class ConnectedUserLogModeExpressionTest {

    private static final long PROJECT_ID = 9L;
    private static final Method TO_STRING_METHOD = ReflectionUtils.findMethod(Object.class, "toString");
    private static final long RESOURCE_ID = 5L;
    private static final String RESOURCE_TYPE = "ProjectDeployment";
    private static final String SCOPE = "DEPLOYMENT_EDIT";
    private static final String WORKFLOW_ID = "workflow-1";
    private static final long WORKSPACE_ID = 1L;

    private ApplicationProperties applicationProperties;
    private CurrentUserResolver currentUserResolver;
    private AutomationPermissionEvaluator evaluator;
    private ProjectRepository projectRepository;
    private AutomationMethodSecurityExpressionRoot root;
    private WorkspaceScopeCacheService workspaceScopeCacheService;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();

        applicationProperties.setSecurity(new ApplicationProperties.Security());

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
            beanFactory.getBeanProvider(ConnectedUserAccessDecider.class), applicationProperties);

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
    void testHasWorkspaceScopeInEnvironmentStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(
            () -> root.hasWorkspaceScopeInEnvironment(WORKSPACE_ID, SCOPE, Environment.PRODUCTION));
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 2L));
        assertLogGrantsAndEnforceDenies(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, null));
        assertLogGrantsAndEnforceDenies(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 99L));
    }

    @Test
    void testHasResourceScopeInEnvironmentStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(
            () -> root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION));
        assertLogGrantsAndEnforceDenies(
            () -> root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null));
    }

    @Test
    void testHasResourceScopeInEnvironmentIdStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(
            () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, 2L));
        assertLogGrantsAndEnforceDenies(
            () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null));
        assertLogGrantsAndEnforceDenies(
            () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, 99L));
    }

    @Test
    void testHasWorkflowScopeStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(() -> root.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_VIEW"));
    }

    @Test
    void testHasWorkflowScopeInEnvironmentStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(
            () -> root.hasWorkflowScopeInEnvironment(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION));
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironment(
                WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT));
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", 0L));
        assertLogGrantsAndEnforceDenies(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", null));
        assertLogGrantsAndEnforceDenies(
            () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", 99L));
    }

    @Test
    void testHasPermissionByIdStaysGrantedUnderSkipInLogMode() throws Throwable {
        assertLogGrantsAndEnforceDenies(() -> evaluator.hasPermission(null, RESOURCE_ID, RESOURCE_TYPE, SCOPE));
    }

    @Test
    void testHasPermissionOnAProjectDeploymentStaysGrantedUnderSkipInLogMode() throws Throwable {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setProjectId(PROJECT_ID);

        ProjectDeploymentDTO projectDeploymentDTO = new ProjectDeploymentDTO(projectDeployment);

        assertLogGrantsAndEnforceDenies(
            () -> evaluator.hasPermission(null, projectDeploymentDTO, "DEPLOYMENT_CREATE"));
    }

    private void assertLogGrantsAndEnforceDenies(BooleanSupplier check) throws Throwable {
        givenMode(ConnectedUserAuthorizationMode.LOG);

        assertThat(AutomationAuthorizationContext.callSkippingChecks(check::getAsBoolean)).isTrue();

        givenMode(ConnectedUserAuthorizationMode.ENFORCE);

        assertThat(AutomationAuthorizationContext.callSkippingChecks(check::getAsBoolean)).isFalse();

        verifyNoInteractions(currentUserResolver, projectRepository, workspaceScopeCacheService);
    }

    private void givenMode(ConnectedUserAuthorizationMode mode) {
        ApplicationProperties.Security security = applicationProperties.getSecurity();

        security.setConnectedUserAuthorizationMode(mode);
    }
}

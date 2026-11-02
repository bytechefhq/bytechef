/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security.ConnectedUserAuthorizationMode;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceConnectedUserTest {

    private static final long USER_ID = 42L;
    private static final String WORKFLOW_ID = "workflow-1";

    private ApplicationProperties applicationProperties;
    private CurrentUserResolver currentUserResolver;
    private ConnectedUserAccessDecider decider;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger permissionServiceLogger;
    private PermissionServiceImpl permissionService;
    private ProjectRepository projectRepository;
    private WorkspaceScopeCacheService workspaceScopeCacheService;
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();

        applicationProperties.setSecurity(new ApplicationProperties.Security());

        currentUserResolver = mock(CurrentUserResolver.class);
        decider = mock(ConnectedUserAccessDecider.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of("connectedUserAccessDecider", decider));

        permissionService = new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(), List.of(),
            beanFactory.getBeanProvider(ConnectedUserAccessDecider.class), applicationProperties);

        logAppender = new ListAppender<>();

        logAppender.start();

        permissionServiceLogger = (Logger) LoggerFactory.getLogger(PermissionServiceImpl.class);

        permissionServiceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        permissionServiceLogger.detachAppender(logAppender);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testLogModeKeepsSkipBehaviourAndLogsTheWouldBeDenial() throws Throwable {
        givenMode(ConnectedUserAuthorizationMode.LOG);
        when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.DENY);

        boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

        assertThat(allowed).isTrue();
        assertThat(logMessages()).anyMatch(message -> message.contains("would deny"));
    }

    @Test
    void testLogModeLogsEachWouldBeDenialOnce() throws Throwable {
        givenMode(ConnectedUserAuthorizationMode.LOG);
        when(decider.decide(any(Serializable.class), eq("ProjectDeployment"), eq("DEPLOYMENT_EDIT")))
            .thenReturn(Decision.DENY);

        AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));
        AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasResourceScope(6L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

        assertThat(logMessages()).filteredOn(message -> message.contains("would deny"))
            .hasSize(1);
    }

    @Test
    void testLogModeReturnsTheDecisionOutsideSkip() {
        givenMode(ConnectedUserAuthorizationMode.LOG);
        when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.DENY);
        when(decider.decide(6L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.GRANT);

        assertThat(permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).isFalse();
        assertThat(permissionService.hasResourceScope(6L, "ProjectDeployment", "DEPLOYMENT_EDIT")).isTrue();
        assertThat(logMessages()).anyMatch(message -> message.contains("denied ProjectDeployment:DEPLOYMENT_EDIT"));
    }

    @Test
    void testEnforceModeReturnsTheDecision() throws Throwable {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.DENY);

        boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

        assertThat(allowed).isFalse();
    }

    @Test
    void testNotGovernedFallsThroughToTodaysLogic() {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decideWorkspace(1L, "WORKFLOW_VIEW")).thenReturn(Decision.NOT_GOVERNED);
        when(decider.decideWorkspace(0L, "")).thenReturn(Decision.NOT_GOVERNED);
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), eq(1L))).thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
    }

    @Test
    void testNotGovernedSkipStillShortCircuits() throws Throwable {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.NOT_GOVERNED);

        boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

        assertThat(allowed).isTrue();
        verifyNoInteractions(currentUserResolver, workspaceScopeCacheService);
    }

    @Test
    void testGovernedPrincipalNeverReachesWorkspaceScopes() {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decideWorkspace(1L, "WORKFLOW_VIEW")).thenReturn(Decision.DENY);

        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isFalse();
        verifyNoInteractions(currentUserResolver, workspaceScopeCacheService);
    }

    @Test
    void testIsAuthorizationSkippedIsFalseForAGovernedPrincipalEvenUnderSkip() throws Throwable {
        givenGovernedPrincipal();

        assertThat(AutomationAuthorizationContext.callSkippingChecks(permissionService::isAuthorizationSkipped))
            .isFalse();
    }

    @Test
    void testIsAuthorizationSkippedFollowsSkipForANotGovernedPrincipal() throws Throwable {
        when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.NOT_GOVERNED);

        assertThat(AutomationAuthorizationContext.callSkippingChecks(permissionService::isAuthorizationSkipped))
            .isTrue();
        assertThat(permissionService.isAuthorizationSkipped()).isFalse();
    }

    @Test
    void testIsAuthorizationSkippedFollowsSkipWithoutADecider() throws Throwable {
        PermissionServiceImpl permissionServiceWithoutDecider = new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(), List.of(),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class), applicationProperties);

        assertThat(
            AutomationAuthorizationContext.callSkippingChecks(permissionServiceWithoutDecider::isAuthorizationSkipped))
                .isTrue();
    }

    @Test
    void testGovernedPrincipalIsNeverTenantAdminOrCurrentUser() {
        givenAdminAuthentication();
        givenGovernedPrincipal();
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));

        for (ConnectedUserAuthorizationMode mode : ConnectedUserAuthorizationMode.values()) {
            givenMode(mode);

            assertThat(permissionService.isTenantAdmin()).isFalse();
            assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();
        }

        verifyNoInteractions(currentUserResolver);
    }

    @Test
    void testGovernedPrincipalIsDeniedEverySkipIgnoringCheckUnderSkip() throws Throwable {
        givenAdminAuthentication();
        givenGovernedPrincipal();
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));

        for (ConnectedUserAuthorizationMode mode : ConnectedUserAuthorizationMode.values()) {
            givenMode(mode);

            AutomationAuthorizationContext.callSkippingChecks(() -> {
                assertThat(permissionService.isTenantAdmin()).isFalse();
                assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();
                assertThat(permissionService.isResourceOwner("ProjectDeployment", 5L)).isFalse();
                assertThat(permissionService.hasWorkspaceRole(1L, "VIEWER")).isFalse();
                assertThat(permissionService.hasResourceRole(5L, "ProjectDeployment", "VIEWER")).isFalse();
                assertThat(permissionService.hasWorkspaceScopeInEveryEnvironment(1L, "WORKFLOW_VIEW")).isFalse();
                assertThat(permissionService.getMyWorkspaceRole(1L)).isNull();
                assertThat(permissionService.getMyWorkspaceScopes(1L)).isEmpty();
                assertThat(permissionService.getMyWorkspaceScopes(1L, Environment.PRODUCTION)).isEmpty();

                return null;
            });
        }

        verifyNoInteractions(currentUserResolver, workspaceScopeCacheService, workspaceUserRepository);
    }

    @Test
    void testGovernedPrincipalPassesWorkflowChecksForAnotherEnvironmentWhenTheDeciderGrants() {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decideWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT")).thenReturn(Decision.GRANT);

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();
        assertThat(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
        assertThat(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_EDIT")).isTrue();
        verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
    }

    @Test
    void testEnforceModeConsultsTheRightDeciderMethodForEveryCheck() {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decide(any(Serializable.class), anyString(), anyString())).thenReturn(Decision.GRANT);
        when(decider.decideInEnvironment(any(Serializable.class), anyString(), anyString(), any(Environment.class)))
            .thenReturn(Decision.GRANT);
        when(decider.decideWorkflow(anyString(), anyString())).thenReturn(Decision.GRANT);
        when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.GRANT);

        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW", Environment.PRODUCTION)).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(9L, "PROJECT_EDIT")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(9L, "PROJECT_EDIT", Environment.PRODUCTION))
            .isTrue();
        assertThat(permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).isTrue();
        assertThat(permissionService.hasResourceScopeInEnvironment(
            5L, "ProjectDeployment", "DEPLOYMENT_EDIT", Environment.PRODUCTION)).isTrue();
        assertThat(permissionService.canUseConnectionInWorkspace(3L, 1L, Environment.PRODUCTION)).isTrue();
        assertThat(permissionService.canUseConnectionInWorkflow(3L, WORKFLOW_ID, Environment.PRODUCTION)).isTrue();

        verify(decider, times(2)).decideWorkspace(1L, "WORKFLOW_VIEW");
        verify(decider).decide(9L, "Project", "PROJECT_EDIT");
        verify(decider).decideInEnvironment(9L, "Project", "PROJECT_EDIT", Environment.PRODUCTION);
        verify(decider).decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT");
        verify(decider).decideInEnvironment(5L, "ProjectDeployment", "DEPLOYMENT_EDIT", Environment.PRODUCTION);
        verify(decider).decideWorkspace(1L, "CONNECTION_VIEW");
        verify(decider).decide(3L, "Connection", "CONNECTION_VIEW");
        verify(decider).decideWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT");
        verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
    }

    @Test
    void testEnforceModeDeniesADeploymentOfTheOwnProjectInAnotherEnvironment() {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decide(9L, "Project", "DEPLOYMENT_CREATE")).thenReturn(Decision.GRANT);
        when(decider.decideInEnvironment(9L, "Project", "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
            .thenReturn(Decision.DENY);

        assertThat(permissionService.hasWorkspaceScopeForProject(9L, "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
            .isFalse();
        verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
    }

    @Test
    void testLogModeAllowsADeploymentOfTheOwnProjectInAnotherEnvironmentUnderSkipAndLogsIt() throws Throwable {
        givenMode(ConnectedUserAuthorizationMode.LOG);
        when(decider.decideInEnvironment(9L, "Project", "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
            .thenReturn(Decision.DENY);

        boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceScopeForProject(9L, "DEPLOYMENT_CREATE", Environment.DEVELOPMENT));

        assertThat(allowed).isTrue();
        assertThat(logMessages()).anyMatch(message -> message.contains("would deny Project:DEPLOYMENT_CREATE"));
    }

    @Test
    void testCanUseConnectionInWorkflowRequiresBothTheConnectionAndTheWorkflow() {
        givenMode(ConnectedUserAuthorizationMode.ENFORCE);
        when(decider.decide(3L, "Connection", "CONNECTION_VIEW")).thenReturn(Decision.GRANT);
        when(decider.decideWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT")).thenReturn(Decision.DENY);
        when(decider.decide(4L, "Connection", "CONNECTION_VIEW")).thenReturn(Decision.DENY);
        when(decider.decideWorkflow("workflow-2", "WORKFLOW_EDIT")).thenReturn(Decision.GRANT);

        assertThat(permissionService.canUseConnectionInWorkflow(3L, WORKFLOW_ID, Environment.PRODUCTION)).isFalse();
        assertThat(permissionService.canUseConnectionInWorkflow(4L, "workflow-2", Environment.PRODUCTION)).isFalse();
        verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
    }

    private void givenAdminAuthentication() {
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    private void givenGovernedPrincipal() {
        when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.DENY);
    }

    private void givenMode(ConnectedUserAuthorizationMode mode) {
        ApplicationProperties.Security security = applicationProperties.getSecurity();

        security.setConnectedUserAuthorizationMode(mode);
    }

    private List<String> logMessages() {
        return logAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}

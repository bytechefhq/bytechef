/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * The environment-aware scope checks. A member who is editor in Development and viewer in Production must be answered
 * per environment, and the environment must come from the argument rather than any ambient state.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceEnvironmentTest {

    private static final String LOGIN = "alice";
    private static final long PROJECT_ID = 100L;
    private static final long USER_ID = 42L;
    private static final long WORKSPACE_ID = 7L;

    private PermissionServiceImpl permissionService;
    private ProjectRepository projectRepository;
    private UserService userService;
    private WorkspaceScopeCacheService workspaceScopeCacheService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

        userService = mock(UserService.class);

        projectRepository = mock(ProjectRepository.class);
        workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        permissionService = new PermissionServiceImpl(
            new CurrentUserResolver(userService), permissionScopeRegistry, projectRepository,
            workspaceScopeCacheService, workspaceUserRepository, List.of(), List.of(), permissiveResolver(),
            List.of(), mock(ObjectProvider.class));

        securityUtilsMock = mockStatic(SecurityUtils.class);

        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(false);
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(LOGIN));

        RequestContextHolder.resetRequestAttributes();

        User user = new User();

        user.setId(USER_ID);
        user.setLogin(LOGIN);

        lenient().when(userService.getUser(LOGIN))
            .thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testAllowsWhenTheEnvironmentRoleGrantsTheScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testDeniesWhenTheEnvironmentRoleDoesNotGrantTheScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testDeniesWhenTheMemberHasNoRoleInThatEnvironment() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testOneMemberIsAnsweredDifferentlyPerEnvironment() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT", "WORKFLOW_VIEW"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testProjectOverloadResolvesTheWorkspaceThenChecksTheEnvironment() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_VIEW", Environment.PRODUCTION))
            .isTrue();
    }

    @Test
    void testDeniesWhenTheProjectIsUnknown() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testTenantAdminIsNotSubjectToPerEnvironmentRoles() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isTrue();
    }

    @Test
    void testEveryEnvironmentCheckRefusesWhenOneEnvironmentLacksTheScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKSPACE_MEMBER_MANAGE"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.STAGING))
            .thenReturn(Set.of());

        // The escalation this closes: a member who administers only Development must not be able to grant a
        // workspace-wide role, which would take effect in Production too.
        assertThat(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .isFalse();
    }

    @Test
    void testEveryEnvironmentCheckAllowsWhenAllEnvironmentsGrantTheScope() {
        for (Environment environment : Environment.values()) {
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, environment))
                .thenReturn(Set.of("WORKSPACE_MEMBER_MANAGE"));
        }

        assertThat(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .isTrue();
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentGrantedUnderSkipMode() throws Throwable {
        // Left unstubbed on purpose: a real evaluation denies (no scope granted), so this only stays true because
        // skip mode short-circuits it. Skip mode is the @SkipAutomationAuthorization delegation, which
        // SkipAutomationAuthorizationAspect declines to arm for an embedded connected user.
        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION));

        assertThat(granted).isTrue();
    }

    @Test
    void testHasWorkspaceScopeInEveryEnvironmentGrantedUnderSkipMode() throws Throwable {
        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"));

        assertThat(granted).isTrue();
    }

    @Test
    void testByIdCheckUsesTheResourcesOwnEnvironment() {
        PermissionServiceImpl permissionServiceWithResolvers = new PermissionServiceImpl(
            new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(deploymentOwnershipResolver()), List.of(), permissiveResolver(),
            List.of(deploymentEnvironmentResolver(Environment.PRODUCTION)), mock(ObjectProvider.class));

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("DEPLOYMENT_VIEW"));

        // The member holds DEPLOYMENT_EDIT somewhere -- the environment-unaware read unions their environments -- but
        // not in Production, where this deployment lives. Without the environment resolver the union would answer the
        // check and a Production viewer could edit a Production deployment.
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isFalse();
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_VIEW"))
            .isTrue();
    }

    @Test
    void testByIdCheckFallsBackWhenNoEnvironmentCanBeResolved() {
        PermissionServiceImpl permissionServiceWithResolvers = new PermissionServiceImpl(
            new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(deploymentOwnershipResolver()), List.of(), permissiveResolver(),
            List.of(deploymentEnvironmentResolver(null)), mock(ObjectProvider.class));

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        // A resolver that cannot answer must never turn a working permission into a failure.
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isTrue();
    }

    private static ResourceOwnershipResolver deploymentOwnershipResolver() {
        return new ResourceOwnershipResolver() {

            @Override
            public String resourceType() {
                return "ProjectDeployment";
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return ResourceOwner.ofWorkspace(WORKSPACE_ID);
            }
        };
    }

    private static ResourceEnvironmentResolver deploymentEnvironmentResolver(Environment environment) {
        return new ResourceEnvironmentResolver() {

            @Override
            public String resourceType() {
                return "ProjectDeployment";
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return Optional.ofNullable(environment);
            }
        };
    }

    private static ResourceVisibilityResolver permissiveResolver() {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .collect(Collectors.toSet());
    }
}

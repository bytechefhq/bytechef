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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceTest {

    private static final long USER_ID = 42L;
    private static final long PROJECT_ID = 100L;
    private static final long WORKSPACE_ID = 7L;
    private static final String LOGIN = "alice";

    private CurrentUserResolver currentUserResolver;
    private PermissionScopeRegistry permissionScopeRegistry;
    private ProjectRepository projectRepository;
    private WorkspaceScopeCacheService workspaceScopeCacheService;
    private UserService userService;
    private WorkspaceUserRepository workspaceUserRepository;
    private PermissionServiceImpl permissionService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
        userService = mock(UserService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        // CurrentUserResolver is the extracted "who is the caller" component (M5 in the RBAC review). It uses the same
        // SecurityUtils + UserService stubs that this test already wires, so we instantiate it directly rather than
        // mocking it — verifying the integration between PermissionServiceImpl and the resolver matters more
        // than isolating either side.
        currentUserResolver = new CurrentUserResolver(userService);

        permissionService = new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(), List.of());

        securityUtilsMock = mockStatic(SecurityUtils.class);

        // Default: not tenant admin, current user is "alice"
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(false);
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(LOGIN));

        // Force the no-request-context branch in getCurrentUserId so we don't depend on RequestContextHolder state.
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
    void testIsTenantAdminTrueWhenAuthorityPresent() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.isTenantAdmin()).isTrue();
    }

    @Test
    void testIsTenantAdminFalseWhenAuthorityAbsent() {
        assertThat(permissionService.isTenantAdmin()).isFalse();
    }

    @Test
    void testIsCurrentUserTrueForMatchingUser() {
        assertThat(permissionService.isCurrentUser(USER_ID)).isTrue();
    }

    @Test
    void testIsCurrentUserFalseForDifferentUser() {
        assertThat(permissionService.isCurrentUser(USER_ID + 1)).isFalse();
    }

    @Test
    void testIsCurrentUserFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();
    }

    @Test
    void testHasWorkspaceRoleShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();

        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceRoleAdminUserSatisfiesAllMinimums() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isTrue();
    }

    @Test
    void testHasWorkspaceRoleViewerCannotEdit() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleFalseForNonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleDeniesAMemberInExplicitMode() {
        // A behaviour change worth pinning: this member holds ADMIN in every environment there is, and is still denied,
        // because a per-environment role is not a workspace-wide one and there is no single role of theirs to compare.
        // The rows are stubbed to show the denial is not "no membership found" — hasWorkspaceRole deliberately never
        // looks at them, which the verify below holds in place. Reached from @PreAuthorize through hasResourceRole, so
        // this denies the connection sharing mutations on WorkspaceConnectionFacadeImpl for such a member.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.DEVELOPMENT),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.STAGING),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasResourceRoleDeniesAMemberInExplicitMode() {
        // hasResourceRole resolves the owning workspace and then asks hasWorkspaceRole, so it inherits that denial.
        // This
        // is the path by which the explicit-mode denial reaches @PreAuthorize: setConnectionVisibility,
        // grantConnectionAccess, revokeConnectionAccess and getConnectionGrants all gate on it, and owning the
        // connection
        // is the remaining way in for such a member.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        PermissionServiceImpl service = createService(
            resolver("Connection", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        assertThat(service.hasResourceRole(1L, "Connection", "ADMIN")).isFalse();

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceRoleNullForAMemberInExplicitMode() {
        // The honest answer, and the other half of the explicit-mode reading: they hold no one role across the
        // workspace,
        // so the members view must render the per-environment roles rather than this.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceRoleFailsClosedOnInvalidRoleOrdinal() {
        // A corrupted or legacy workspace_role ordinal hydrated via Spring Data JDBC (which bypasses the constructor's
        // range validation) must fail closed — deny — rather than throw ArrayIndexOutOfBoundsException as a 500.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(999);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_DELETE")).isTrue();

        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceScopeReturnsTrueWhenScopeGranted() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeReturnsFalseWhenScopeMissing() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_DELETE")).isTrue();

        verify(projectRepository, never()).findById(PROJECT_ID);
    }

    @Test
    void testHasWorkspaceScopeForProjectResolvesOwningWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectFalseForUnknownProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_VIEW")).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceRoleFalseOnUnknownRoleName() {
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "NOT_A_ROLE")).isFalse();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceRoleFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceScopeFalseOnUnknownScopeName() {
        // Scope names are now plain strings validated by set-membership, not by an enum lookup. A name no role grants
        // (a typo, or a scope the user lacks) simply isn't in the resolved set, so the check fails closed without any
        // special-casing.
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "NOT_A_SCOPE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceScopeFalseWhenAuthenticatedLoginHasNoPlatformUser() {
        // Defense-in-depth: an authenticated principal whose login resolves to no platform user (e.g. a non-platform
        // principal reaching a platform RBAC check) must fail closed — deny — rather than bubble UserNotFoundException
        // as a 500. Embedded flows that legitimately bypass automation RBAC are handled earlier via the skip flag.
        when(userService.getUser(LOGIN)).thenThrow(new UserNotFoundException());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceScopesEmptyWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID)).isEmpty();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceScopesReturnsAllScopesForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);
        when(permissionScopeRegistry.getAllScopeNames())
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKFLOW_DELETE", "CONNECTION_VIEW"));

        Set<String> scopes = permissionService.getMyWorkspaceScopes(WORKSPACE_ID);

        assertThat(scopes).containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_DELETE", "CONNECTION_VIEW");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceScopesDelegatesToCache() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID))
            .containsExactly("WORKFLOW_VIEW");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentDelegatesToTheEnvironmentLookup() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.STAGING))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.STAGING))
            .containsExactly("WORKFLOW_EDIT");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentReturnsAllScopesForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);
        when(permissionScopeRegistry.getAllScopeNames())
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKSPACE_MEMBER_MANAGE"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION))
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKSPACE_MEMBER_MANAGE");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentEmptyWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testGetMyWorkspaceRoleNullWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdminForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isEqualTo("ADMIN");
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceRoleReturnsMembershipRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isEqualTo("EDITOR");
    }

    @Test
    void testGetMyWorkspaceRoleReturnsNullForNonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testGetMyWorkspaceRoleNullOnInvalidRoleOrdinal() {
        // Symmetric to hasWorkspaceRole: an out-of-range ordinal must fail closed to a null role rather than throw.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(999);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testHasWorkflowScopeGrantsUnderAutomationAuthorizationSkip() throws Throwable {
        // Embedded → automation delegation (whole ConnectedUserProjectFacadeImpl is @SkipAutomationAuthorization):
        // the principal is an API-key identity with no row in the user table, so resolving it throws
        // UserNotFoundException. The skip flag must short-circuit the gate to grant BEFORE any project/user/cache
        // lookup runs. Without the skip flag this same call returns false (project not stubbed → no workspace).
        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EDIT"));

        assertThat(granted).isTrue();

        verify(projectRepository, never()).findByWorkflowId(anyString());
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkflowScopeFalseWithoutSkipWhenWorkflowUnknown() {
        // Control for the skip test above: outside the skip context the same inputs deny (the workflow id resolves
        // to no project, so there is no owning workspace to grant a scope in).
        assertThat(permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EDIT")).isFalse();
    }

    @Test
    void testHasResourceScopeUsesWorkspaceScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("CONNECTION_DELETE"));

        PermissionServiceImpl service = createService(
            resolver("Connection", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeNoWorkspaceFailsClosed() {
        PermissionServiceImpl service = createService(resolver("Connection", ResourceOwner.unknown()));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testIsResourceOwnerMatchAllows() {
        PermissionServiceImpl service = createService(resolver("ApiKey", ResourceOwner.ofUser(USER_ID)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testIsResourceOwnerMismatchDenies() {
        PermissionServiceImpl service = createService(resolver("ApiKey", ResourceOwner.ofUser(USER_ID + 1)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isFalse();
    }

    @Test
    void testHasResourceRoleChecksWorkspaceRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        PermissionServiceImpl service = createService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isTrue();
    }

    @Test
    void testHasResourceRoleNoWorkspaceFailsClosed() {
        PermissionServiceImpl service = createService(resolver("KnowledgeBase", ResourceOwner.unknown()));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkflowScopeResolvesProjectWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowGrantsWhenTheWorkflowBelongsToNoProject() {
        assertThat(permissionService.hasWorkflowScopeIfProjectWorkflow(
            "integration-workflow", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();

        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowDeniesAProjectWorkflowWithoutTheScope() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowGrantsAProjectWorkflowWithTheScope() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();
    }

    @Test
    void testHasWorkflowScopeDeniesAMemberWhoHoldsTheScopeOnlyInProduction() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_EDIT"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT)).isFalse();
        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT", Environment.PRODUCTION)).isTrue();
        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
    }

    @Test
    void testEvictWorkspaceScopeCacheDelegates() {
        permissionService.evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);

        verify(workspaceScopeCacheService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testEvictAllWorkspaceScopeCacheDelegates() {
        permissionService.evictAllWorkspaceScopeCache();

        verify(workspaceScopeCacheService, times(1)).evictAllWorkspaceScopeCache();
    }

    private PermissionServiceImpl createService(ResourceOwnershipResolver... resolvers) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resolvers), List.of());
    }

    private static ResourceOwnershipResolver resolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }
        };
    }
}

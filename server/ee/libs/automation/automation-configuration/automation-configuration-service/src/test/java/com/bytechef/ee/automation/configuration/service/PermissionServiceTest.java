/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
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

    private ProjectUserRepository projectUserRepository;
    private ProjectScopeCacheService projectScopeCacheService;
    private UserService userService;
    private WorkspaceUserRepository workspaceUserRepository;
    private PermissionServiceImpl permissionService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        projectUserRepository = mock(ProjectUserRepository.class);
        projectScopeCacheService = mock(ProjectScopeCacheService.class);
        userService = mock(UserService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        // CurrentUserResolver is the extracted "who is the caller" component (M5 in the RBAC review). It uses the same
        // SecurityUtils + UserService stubs that this test already wires, so we instantiate it directly rather than
        // mocking it \u2014 verifying the integration between PermissionServiceImpl and the resolver matters more
        // than isolating either side.
        CurrentUserResolver currentUserResolver = new CurrentUserResolver(userService);

        // ObjectProvider with null registry exercises the "lightweight EE app without actuator" branch (counters
        // become no-ops). Tests that assert counter increments explicitly stub a MeterRegistry-backed provider.
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        permissionService = new PermissionServiceImpl(
            currentUserResolver, projectUserRepository, projectScopeCacheService, workspaceUserRepository,
            meterRegistryProvider);

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
    void testHasWorkspaceRoleShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();

        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceRoleAdminUserSatisfiesAllMinimums() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isTrue();
    }

    @Test
    void testHasWorkspaceRoleViewerCannotEdit() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleFalseForNonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasProjectScopeShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasProjectScope(PROJECT_ID, "WORKFLOW_DELETE")).isTrue();

        verify(projectScopeCacheService, never()).getProjectScopes(USER_ID, PROJECT_ID);
    }

    @Test
    void testHasProjectScopeReturnsTrueWhenScopeGranted() {
        when(projectScopeCacheService.getProjectScopes(USER_ID, PROJECT_ID))
            .thenReturn(EnumSet.of(PermissionScope.WORKFLOW_VIEW, PermissionScope.WORKFLOW_EDIT));

        assertThat(permissionService.hasProjectScope(PROJECT_ID, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasProjectScope(PROJECT_ID, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasProjectScopeReturnsFalseWhenScopeMissing() {
        when(projectScopeCacheService.getProjectScopes(USER_ID, PROJECT_ID))
            .thenReturn(EnumSet.of(PermissionScope.WORKFLOW_VIEW));

        assertThat(permissionService.hasProjectScope(PROJECT_ID, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasProjectRoleAdminSatisfiesAllMinimums() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN)));

        assertThat(permissionService.hasProjectRole(PROJECT_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "OPERATOR")).isTrue();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "EDITOR")).isTrue();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "ADMIN")).isTrue();
    }

    @Test
    void testHasProjectRoleOperatorCannotEdit() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.OPERATOR)));

        assertThat(permissionService.hasProjectRole(PROJECT_ID, "OPERATOR")).isTrue();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "EDITOR")).isFalse();
    }

    @Test
    void testHasProjectRoleFalseForCustomRoleUserEvenWithManageUsersScope() {
        // Custom roles are intentionally excluded from the built-in role hierarchy. A custom role granting
        // PROJECT_MANAGE_USERS used to be treated as effective ADMIN here, which gave custom-role-only members a
        // self-promotion path through addProjectUser(..., self, ADMIN) / updateProjectUserRole. Keeping the check
        // strictly built-in means a custom-role holder cannot grant or demote built-in roles. Orphaning guards
        // (isEffectiveAdmin) continue to count custom-role admins — that's a separate concern from this elevation
        // path.
        ProjectUser customRoleUser = ProjectUser.forCustomRole(PROJECT_ID, USER_ID, 999L);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(customRoleUser));

        assertThat(permissionService.hasProjectRole(PROJECT_ID, "VIEWER")).isFalse();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "OPERATOR")).isFalse();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "EDITOR")).isFalse();
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "ADMIN")).isFalse();
    }

    @Test
    void testHasProjectRoleFalseForCustomRoleUserWithoutManageUsersScope() {
        ProjectUser customRoleUser = ProjectUser.forCustomRole(PROJECT_ID, USER_ID, 999L);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(customRoleUser));

        assertThat(permissionService.hasProjectRole(PROJECT_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasProjectRoleFalseForNonMember() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasProjectRole(PROJECT_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasProjectRoleFalseOnUnknownRoleName() {
        // A typo in a @PreAuthorize literal must fail closed rather than bubble out as HTTP 500.
        assertThat(permissionService.hasProjectRole(PROJECT_ID, "NOT_A_ROLE")).isFalse();
        verify(projectUserRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void testHasProjectRoleFalseWhenSecurityContextEmpty() {
        // Load-bearing fail-closed branch: no SecurityContext must deny, never allow.
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasProjectRole(PROJECT_ID, "VIEWER")).isFalse();
        verify(projectUserRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceRoleFalseOnUnknownRoleName() {
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "NOT_A_ROLE")).isFalse();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceId(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceRoleFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceId(anyLong(), anyLong());
    }

    @Test
    void testHasProjectScopeFalseOnUnknownScopeName() {
        assertThat(permissionService.hasProjectScope(PROJECT_ID, "NOT_A_SCOPE")).isFalse();
        verify(projectScopeCacheService, never()).getProjectScopes(anyLong(), anyLong());
    }

    @Test
    void testHasProjectScopeFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasProjectScope(PROJECT_ID, "WORKFLOW_VIEW")).isFalse();
        verify(projectScopeCacheService, never()).getProjectScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyProjectScopesEmptyWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyProjectScopes(PROJECT_ID)).isEmpty();
        verify(projectScopeCacheService, never()).getProjectScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceRoleNullWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceId(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceRoleNullOnCorruptedOrdinal() {
        // A workspace_user.workspace_role value outside the enum range (e.g., a row written by a newer binary and
        // read by an older one) must fail closed instead of throwing ArrayIndexOutOfBoundsException. The constructor
        // validates the ordinal at write time, so mock the getter to simulate a corrupted DB read.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(99);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testGetMyProjectScopesReturnsAllScopesForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        Set<String> scopes = permissionService.getMyProjectScopes(PROJECT_ID);

        assertThat(scopes).hasSize(PermissionScope.values().length);
        assertThat(scopes).contains(PermissionScope.WORKFLOW_VIEW.name(), PermissionScope.WORKFLOW_DELETE.name());
        verify(projectScopeCacheService, never()).getProjectScopes(USER_ID, PROJECT_ID);
    }

    @Test
    void testGetMyProjectScopesDelegatesToCache() {
        when(projectScopeCacheService.getProjectScopes(USER_ID, PROJECT_ID))
            .thenReturn(EnumSet.of(PermissionScope.WORKFLOW_VIEW));

        assertThat(permissionService.getMyProjectScopes(PROJECT_ID))
            .containsExactly(PermissionScope.WORKFLOW_VIEW.name());
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdminForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isEqualTo("ADMIN");
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceRoleReturnsMembershipRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isEqualTo("EDITOR");
    }

    @Test
    void testGetMyWorkspaceRoleReturnsNullForNonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testEvictProjectScopeCacheDelegates() {
        permissionService.evictProjectScopeCache(USER_ID, PROJECT_ID);

        verify(projectScopeCacheService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testEvictAllProjectScopeCacheDelegates() {
        permissionService.evictAllProjectScopeCache();

        verify(projectScopeCacheService, times(1)).evictAllProjectScopeCache();
    }
}

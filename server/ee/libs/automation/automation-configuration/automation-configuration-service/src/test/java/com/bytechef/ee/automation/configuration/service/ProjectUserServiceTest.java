/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies business rules of the EE {@link ProjectUserServiceImpl}: validation, role-elevation guard, last-admin
 * protection (including custom-role admins), cache eviction, and role-update semantics. {@code @PreAuthorize} is not
 * exercised here — it requires going through the Spring proxy. Direct method security coverage lives in
 * security/integration tests.
 *
 * <p>
 * Design note: the setUp method does NOT globally stub {@code isTenantAdmin → true}. That default would short-circuit
 * {@code validateRoleNotExceedsCallerLevel} on every test, silently masking regressions in the role-elevation guard
 * (which is the central privilege-escalation defense). Individual tests that don't care about the guard and happen to
 * exercise an add/update path flip the stub to {@code true} in their own setup.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectUserServiceTest {

    private static final long PROJECT_ID = 100L;
    private static final long WORKSPACE_ID = 7L;
    private static final long USER_ID = 42L;
    private static final long CUSTOM_ROLE_ID = 900L;

    private CustomRoleScopeResolver customRoleScopeResolver;
    private PermissionService permissionService;
    private ProjectRepository projectRepository;
    private ProjectUserRepository projectUserRepository;
    private WorkspaceUserRepository workspaceUserRepository;
    private ProjectUserServiceImpl projectUserService;

    @BeforeEach
    void setUp() {
        customRoleScopeResolver = mock(CustomRoleScopeResolver.class);
        permissionService = mock(PermissionService.class);
        projectRepository = mock(ProjectRepository.class);
        projectUserRepository = mock(ProjectUserRepository.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        projectUserService = new ProjectUserServiceImpl(
            customRoleScopeResolver, permissionService, projectUserRepository, projectRepository,
            workspaceUserRepository);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Role-elevation guard
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void testAddProjectUserRejectsCallerGrantingHigherRoleThanTheirOwn() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasProjectRole(PROJECT_ID, ProjectRole.ADMIN.name())).thenReturn(false);

        assertThatThrownBy(() -> projectUserService.addProjectUser(PROJECT_ID, USER_ID, ProjectRole.ADMIN.ordinal()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("equal-or-higher role");

        verify(projectUserRepository, never()).save(any(ProjectUser.class));
    }

    @Test
    void testUpdateProjectUserRoleRejectsGrantingAboveCallerLevel() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasProjectRole(PROJECT_ID, ProjectRole.ADMIN.name())).thenReturn(false);

        assertThatThrownBy(() -> projectUserService.updateProjectUserRole(
            PROJECT_ID, USER_ID, ProjectRole.ADMIN.ordinal()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("equal-or-higher role");
    }

    @Test
    void testTenantAdminBypassesRoleElevationGuard() {
        Project project = buildProject();

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());
        when(projectRepository.findById(PROJECT_ID))
            .thenReturn(Optional.of(project));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.addProjectUser(PROJECT_ID, USER_ID, ProjectRole.ADMIN.ordinal());

        // A tenant admin can grant ADMIN without holding ADMIN themselves on the project
        verify(permissionService, never()).hasProjectRole(anyLong(), anyString());
        verify(projectUserRepository, times(1)).save(any(ProjectUser.class));
    }

    @Test
    void testAddProjectUserRejectsNegativeProjectRoleOrdinal() {
        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(() -> projectUserService.addProjectUser(PROJECT_ID, USER_ID, -1))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("out of range");

        verify(projectUserRepository, never()).save(any(ProjectUser.class));
    }

    @Test
    void testAddProjectUserRejectsOutOfRangeProjectRoleOrdinal() {
        when(permissionService.isTenantAdmin()).thenReturn(false);

        int outOfRange = ProjectRole.values().length + 5;

        assertThatThrownBy(() -> projectUserService.addProjectUser(PROJECT_ID, USER_ID, outOfRange))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("out of range");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // addProjectUser
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void testAddProjectUserPersistsAndEvictsCache() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        Project project = buildProject();

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());
        when(projectRepository.findById(PROJECT_ID))
            .thenReturn(Optional.of(project));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.addProjectUser(PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal());

        ArgumentCaptor<ProjectUser> captor = ArgumentCaptor.forClass(ProjectUser.class);

        verify(projectUserRepository).save(captor.capture());

        ProjectUser saved = captor.getValue();

        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getProjectRole()).isEqualTo(ProjectRole.EDITOR.ordinal());

        verify(permissionService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testAddProjectUserRejectsDuplicate() {
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.VIEWER)));

        assertThatThrownBy(() -> projectUserService.addProjectUser(PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already a member");

        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(permissionService, never()).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testAddProjectUserRejectsUnknownProject() {
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());
        when(projectRepository.findById(PROJECT_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectUserService.addProjectUser(PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not found");

        verify(projectUserRepository, never()).save(any(ProjectUser.class));
    }

    @Test
    void testAddProjectUserRejectsNonWorkspaceMember() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        Project project = buildProject();

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());
        when(projectRepository.findById(PROJECT_ID))
            .thenReturn(Optional.of(project));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectUserService.addProjectUser(PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not a member of the workspace");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // deleteProjectUser — last-effective-admin guard
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void testDeleteProjectUserSucceedsForNonAdmin() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.EDITOR)));

        projectUserService.deleteProjectUser(PROJECT_ID, USER_ID);

        verify(projectUserRepository, times(1)).deleteByProjectIdAndUserId(PROJECT_ID, USER_ID);
        verify(permissionService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testDeleteProjectUserBlockedWhenLastBuiltInAdmin() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN)));
        when(projectUserRepository.countEffectiveAdmins(
            PROJECT_ID, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(1L);

        assertThatThrownBy(() -> projectUserService.deleteProjectUser(PROJECT_ID, USER_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(projectUserRepository, never()).deleteByProjectIdAndUserId(PROJECT_ID, USER_ID);
    }

    @Test
    void testDeleteProjectUserBlockedWhenLastCustomRoleAdmin() {
        ProjectUser customRoleMember = ProjectUser.forCustomRole(PROJECT_ID, USER_ID, CUSTOM_ROLE_ID);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(customRoleMember));
        when(customRoleScopeResolver.resolveScopes(CUSTOM_ROLE_ID))
            .thenReturn(Optional.of(EnumSet.of(PermissionScope.PROJECT_MANAGE_USERS)));
        when(projectUserRepository.countEffectiveAdmins(
            PROJECT_ID, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(1L);

        assertThatThrownBy(() -> projectUserService.deleteProjectUser(PROJECT_ID, USER_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(projectUserRepository, never()).deleteByProjectIdAndUserId(PROJECT_ID, USER_ID);
    }

    @Test
    void testDeleteProjectUserAllowedWhenAnotherEffectiveAdminExists() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN)));
        when(projectUserRepository.countEffectiveAdmins(
            PROJECT_ID, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(2L);

        projectUserService.deleteProjectUser(PROJECT_ID, USER_ID);

        verify(projectUserRepository, times(1)).deleteByProjectIdAndUserId(PROJECT_ID, USER_ID);
    }

    @Test
    void testDeleteCustomRoleMemberWithoutAdminScopeSkipsLastAdminGuard() {
        ProjectUser customRoleMember = ProjectUser.forCustomRole(PROJECT_ID, USER_ID, CUSTOM_ROLE_ID);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(customRoleMember));
        when(customRoleScopeResolver.resolveScopes(CUSTOM_ROLE_ID))
            .thenReturn(Optional.of(EnumSet.of(PermissionScope.WORKFLOW_VIEW)));

        projectUserService.deleteProjectUser(PROJECT_ID, USER_ID);

        verify(projectUserRepository, never()).countEffectiveAdmins(anyLong(), anyInt(), anyString());
        verify(projectUserRepository, times(1)).deleteByProjectIdAndUserId(PROJECT_ID, USER_ID);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // updateProjectUserRole
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void testUpdateProjectUserRoleChangesRoleAndEvictsCache() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.EDITOR);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.updateProjectUserRole(PROJECT_ID, USER_ID, ProjectRole.VIEWER.ordinal());

        assertThat(projectUser.getProjectRole()).isEqualTo(ProjectRole.VIEWER.ordinal());
        verify(permissionService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testUpdateProjectUserRoleBlockedWhenDemotingLastAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(projectUserRepository.countEffectiveAdmins(
            PROJECT_ID, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(1L);

        assertThatThrownBy(() -> projectUserService.updateProjectUserRole(
            PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("last admin");

        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(permissionService, never()).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testUpdateProjectUserRoleAllowsAdminToAdminUpdate() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.updateProjectUserRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN.ordinal());

        verify(projectUserRepository, never())
            .countEffectiveAdmins(anyLong(), anyInt(), anyString());
        verify(permissionService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testUpdateProjectUserRoleRejectsUnknownMembership() {
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectUserService.updateProjectUserRole(
            PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("not a member");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // updateProjectUserRole — self-demotion guard
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void testUpdateProjectUserRoleBlocksSelfDemotionEvenWhenAnotherAdminExists() {
        // Even though another ADMIN row exists (countEffectiveAdmins > 1), the caller demoting themselves would
        // instantly lose PROJECT_MANAGE_USERS with no way to reverse it. Require another admin to perform the
        // demotion so a human is in the loop confirming the survivor is real.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasProjectRole(PROJECT_ID, ProjectRole.EDITOR.name())).thenReturn(true);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));

        assertThatThrownBy(() -> projectUserService.updateProjectUserRole(
            PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Cannot demote your own role");

        verify(projectUserRepository, never()).save(any(ProjectUser.class));
        verify(projectUserRepository, never()).countEffectiveAdmins(anyLong(), anyInt(), anyString());
    }

    @Test
    void testUpdateProjectUserRoleAllowsAnotherAdminToDemoteYou() {
        // Caller != subject. Since countEffectiveAdmins returns 2, the last-admin guard passes and the demotion
        // succeeds.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasProjectRole(PROJECT_ID, ProjectRole.EDITOR.name())).thenReturn(true);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(false);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(projectUserRepository.countEffectiveAdmins(
            PROJECT_ID, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(2L);
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.updateProjectUserRole(PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal());

        assertThat(projectUser.getProjectRole()).isEqualTo(ProjectRole.EDITOR.ordinal());
        verify(permissionService, times(1)).evictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testUpdateProjectUserRoleAllowsTenantAdminToSelfDemote() {
        // Tenant admins are exempt from the self-demotion guard — they can always restore themselves outside the
        // per-project RBAC surface.
        when(permissionService.isTenantAdmin()).thenReturn(true);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(projectUserRepository.countEffectiveAdmins(
            PROJECT_ID, ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(2L);
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.updateProjectUserRole(PROJECT_ID, USER_ID, ProjectRole.EDITOR.ordinal());

        verify(projectUserRepository, times(1)).save(any(ProjectUser.class));
    }

    @Test
    void testUpdateProjectUserRoleAllowsSelfNoOpUpdate() {
        // Idempotent self-update (admin → admin) must succeed — the UI sometimes re-submits the current role when a
        // non-role field changes on the same mutation surface. Only an actual downgrade is rejected.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasProjectRole(PROJECT_ID, ProjectRole.ADMIN.name())).thenReturn(true);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(projectUserRepository.save(any(ProjectUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        projectUserService.updateProjectUserRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN.ordinal());

        verify(projectUserRepository, times(1)).save(any(ProjectUser.class));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Read methods
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    void testGetProjectUsersDelegatesToRepository() {
        List<ProjectUser> expected = List.of(
            ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.ADMIN),
            ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID + 1, ProjectRole.VIEWER));

        when(projectUserRepository.findAllByProjectId(PROJECT_ID))
            .thenReturn(expected);

        assertThat(projectUserService.getProjectUsers(PROJECT_ID)).containsExactlyElementsOf(expected);
    }

    @Test
    void testCountByCustomRoleIdDelegatesToRepository() {
        when(projectUserRepository.countByCustomRoleId(99L))
            .thenReturn(3L);

        assertThat(projectUserService.countByCustomRoleId(99L)).isEqualTo(3L);
    }

    private static Project buildProject() {
        Project project = new Project();

        project.setName("Test");
        project.setWorkspaceId(WORKSPACE_ID);

        return project;
    }
}

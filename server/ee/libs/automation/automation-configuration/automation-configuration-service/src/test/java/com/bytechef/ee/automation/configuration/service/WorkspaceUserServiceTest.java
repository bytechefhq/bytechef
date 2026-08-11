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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the last-admin guard, self-demotion guard, and cache eviction in {@link WorkspaceUserServiceImpl}.
 * {@code @PreAuthorize} enforcement is verified separately in {@link PreAuthorizeAnnotationTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserServiceTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long USER_ID = 42L;

    private CustomRoleRepository customRoleRepository;
    private PermissionService permissionService;
    private UserInvitationService userInvitationService;
    private UserService userService;
    private WorkspaceService workspaceService;
    private WorkspaceUserRepository workspaceUserRepository;
    private WorkspaceUserServiceImpl workspaceUserService;

    @BeforeEach
    void setUp() {
        customRoleRepository = mock(CustomRoleRepository.class);
        permissionService = mock(PermissionService.class);
        userInvitationService = mock(UserInvitationService.class);
        userService = mock(UserService.class);
        workspaceService = mock(WorkspaceService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        workspaceUserService = new WorkspaceUserServiceImpl(
            customRoleRepository, permissionService, userInvitationService, userService, workspaceService,
            mock(WorkspaceUserAuditPublisher.class), workspaceUserRepository);
    }

    @Test
    void testAddWorkspaceUserPersists() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());

        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testAddWorkspaceUserRejectsDuplicate() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already a member");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
        verify(permissionService, never()).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testAddWorkspaceUserAcceptsACustomRole() {
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Deployer", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, 900L);

        assertThat(result.getCustomRoleId()).isEqualTo(900L);

        // The XOR invariant holds on creation too, not just on a later conversion.
        assertThat(result.getWorkspaceRole()).isNull();

        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testAddWorkspaceUserRequiresExactlyOneRole() {
        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, null))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Exactly one");

        assertThatThrownBy(
            () -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, 900L))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Exactly one");

        // Typed rather than the domain constructor's IllegalArgumentException, which would surface as a 500.
        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testInviteWorkspaceUserAcceptsACustomRole() {
        User invitedUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("newcomer@example.com")).thenReturn(Optional.empty());
        when(userInvitationService.inviteUser("newcomer@example.com", "ROLE_USER")).thenReturn(invitedUser);
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Deployer", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.inviteWorkspaceUser(
            WORKSPACE_ID, "newcomer@example.com", null, 900L);

        assertThat(result.getCustomRoleId()).isEqualTo(900L);
        assertThat(result.getWorkspaceRole()).isNull();

        // One transaction: the invitee lands on the role that was asked for, not on a built-in fallback.
        verify(workspaceUserRepository, times(1)).save(any(WorkspaceUser.class));
    }

    @Test
    void testRemoveWorkspaceUserThrowsWhenNotMember() {
        // Non-member removal was previously a silent no-op returning false; it now throws NOT_MEMBER so the caller
        // can distinguish "nothing to remove" from "removal succeeded", and the GraphQL layer can surface a typed
        // error instead of a confusing success toast on stale UI.
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not a member");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserBlockedWhenLastAdmin() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserDeletesAndEvictsCache() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();

        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserAllowedWhenAnotherAdminExists() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();
        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testUpdateWorkspaceUserRoleChangesRole() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.VIEWER.ordinal());
        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testUpdateWorkspaceUserRoleBlockedWhenDemotingLastAdmin() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsAdminToAdminUpdate() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN);

        verify(workspaceUserRepository, never())
            .countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleRejectsUnknownMembership() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void testUpdateWorkspaceUserRoleBlocksSelfDemotionEvenWhenAnotherAdminExists() {
        // Even with a second ADMIN row present, the caller demoting themselves would instantly lose
        // workspace-management privileges with no safe way to recover. Require another admin to perform the demotion.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Cannot demote your own role");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
        verify(workspaceUserRepository, never())
            .countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsAnotherAdminToDemoteYou() {
        // Caller != subject: the demotion proceeds (and last-admin guard fires on its own terms).
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsTenantAdminToSelfDemote() {
        // Tenant admins are exempt from the workspace-level self-demotion guard — they retain the ability to
        // restore themselves outside workspace RBAC.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        verify(workspaceUserRepository, times(1)).save(any(WorkspaceUser.class));
    }

    @Test
    void testCountByCustomRoleIdDelegatesToRepository() {
        when(workspaceUserRepository.countByCustomRoleId(900L)).thenReturn(4L);

        assertThat(workspaceUserService.countByCustomRoleId(900L)).isEqualTo(4L);

        verify(workspaceUserRepository, times(1)).countByCustomRoleId(900L);
    }

    @Test
    void testInviteWorkspaceUserProvisionsAnUnknownEmail() {
        User invitedUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("newcomer@example.com")).thenReturn(Optional.empty());
        when(userInvitationService.inviteUser("newcomer@example.com", "ROLE_USER")).thenReturn(invitedUser);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.inviteWorkspaceUser(
            WORKSPACE_ID, "newcomer@example.com", WorkspaceRole.EDITOR);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());

        // The land-nowhere defect, from the workspace side: an invite that provisioned an account without writing
        // membership is exactly what this must never do again.
        verify(workspaceUserRepository, times(1)).save(any(WorkspaceUser.class));
        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testInviteWorkspaceUserReusesAnExistingAccount() {
        User existingUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("colleague@example.com")).thenReturn(Optional.of(existingUser));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceService.getWorkspace(WORKSPACE_ID)).thenReturn(createWorkspace("Engineering"));

        workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER);

        // Reusing rather than rejecting: a workspace admin should not have to know whether a colleague already
        // signed up, and re-provisioning would consume a second seat for one person.
        verify(userInvitationService, never()).inviteUser(any(), any());
        verify(workspaceUserRepository, times(1)).save(any(WorkspaceUser.class));
    }

    @Test
    void testInviteWorkspaceUserNotifiesAnExistingAccount() {
        User existingUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("colleague@example.com")).thenReturn(Optional.of(existingUser));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceService.getWorkspace(WORKSPACE_ID)).thenReturn(createWorkspace("Engineering"));

        workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER);

        // Without this the existing account holder is added silently and discovers the workspace by chance. They
        // must not get the claim link -- they already have a password.
        verify(userInvitationService).notifyAddedToWorkspace(existingUser, "Engineering");
    }

    @Test
    void testInviteWorkspaceUserDoesNotNotifyANewlyProvisionedAccount() {
        User invitedUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("newcomer@example.com")).thenReturn(Optional.empty());
        when(userInvitationService.inviteUser("newcomer@example.com", "ROLE_USER")).thenReturn(invitedUser);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "newcomer@example.com", WorkspaceRole.EDITOR);

        // A new account already learned about this through the claim link; a second mail would be noise.
        verify(userInvitationService, never()).notifyAddedToWorkspace(any(), any());
    }

    @Test
    void testInviteWorkspaceUserDoesNotNotifyWhenTheAddFails() {
        User existingUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("colleague@example.com")).thenReturn(Optional.of(existingUser));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(
                WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER))
                    .isInstanceOf(ConfigurationException.class);

        // Announcing access nobody has would be worse than announcing nothing.
        verify(userInvitationService, never()).notifyAddedToWorkspace(any(), any());
    }

    @Test
    void testInviteWorkspaceUserRejectsAnExistingMember() {
        User existingUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("colleague@example.com")).thenReturn(Optional.of(existingUser));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(
                WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("already a member");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    private User createUser(long id) {
        User user = new User();

        user.setId(id);

        return user;
    }

    @Test
    void testAssignCustomRoleAcceptsAnExistingRole() {
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L);

        assertThat(result.getCustomRoleId()).isEqualTo(900L);

        // The XOR invariant: a member holds a built-in role or a custom one, never both.
        assertThat(result.getWorkspaceRole()).isNull();

        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testAssignCustomRoleRejectsAnUnknownRoleId() {
        // The workspace-boundary check is gone with the per-workspace tier, but a dangling custom_role_id would
        // fail closed at permission-check time and invisibly lock the member out — writes must still reject it
        // loudly.
        when(customRoleRepository.findById(900L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("does not exist");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAssignCustomRoleRejectsDemotingTheLastAdmin() {
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        // A custom role's scopes are no guarantee it can manage anything, so converting the last admin locks the
        // workspace out exactly as demoting them to EDITOR would.
        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testRemoveWorkspaceUserNamesTenantAdminAccessRatherThanClaimingNonMembership() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(userService.getUsersByAuthorityName("ROLE_ADMIN")).thenReturn(List.of(createUser(USER_ID)));

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("as a tenant admin");

        // "Not a member" describes the row, not the access — misleading for someone who demonstrably administers
        // the workspace and appears in its members view as an inherited entry.
    }

    private Workspace createWorkspace(String name) {
        Workspace workspace = new Workspace();

        workspace.setName(name);

        return workspace;
    }
}

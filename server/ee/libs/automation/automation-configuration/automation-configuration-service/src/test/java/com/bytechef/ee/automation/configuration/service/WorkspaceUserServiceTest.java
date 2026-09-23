/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Covers the last-admin guard, the self-demotion and self-promotion guards, the limit on granting more than the caller
 * holds, which row each workspace-wide operation reads now that a member may hold several, and cache eviction in
 * {@link WorkspaceUserServiceImpl}. {@code @PreAuthorize} enforcement is verified separately in
 * {@link PreAuthorizeAnnotationTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserServiceTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long USER_ID = 42L;

    private CustomRoleRepository customRoleRepository;
    private PermissionScopeRegistry permissionScopeRegistry;
    private PermissionService permissionService;
    private UserInvitationService userInvitationService;
    private UserService userService;
    private WorkspaceService workspaceService;
    private WorkspaceUserRepository workspaceUserRepository;
    private WorkspaceUserServiceImpl workspaceUserService;

    @BeforeEach
    void setUp() {
        customRoleRepository = mock(CustomRoleRepository.class);
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        permissionService = mock(PermissionService.class);
        userInvitationService = mock(UserInvitationService.class);
        userService = mock(UserService.class);
        workspaceService = mock(WorkspaceService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        // Every invite test exercises a workspace that exists unless it says otherwise; only the not-found tests
        // override this.
        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenReturn(true);

        // The caller holds every scope in every environment unless a test says otherwise; only the grant-limit tests
        // narrow this.
        when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any(Environment.class))).thenReturn(true);
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), anyString())).thenReturn(true);

        workspaceUserService = new WorkspaceUserServiceImpl(
            customRoleRepository, permissionScopeRegistry, permissionService, userInvitationService, userService,
            workspaceService, mock(WorkspaceUserAuditPublisher.class), workspaceUserRepository);
    }

    @Test
    void testAddWorkspaceUserPersists() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.EDITOR.ordinal());

        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testAddWorkspaceUserRejectsAnUnknownWorkspace() {
        // The tenant-admin path: hasWorkspaceScopeInEveryEnvironment grants unconditionally for a tenant admin, and the
        // already-a-member probe answers false for a workspace that does not exist, so without this check the insert
        // reaches the unconditional workspace foreign key and surfaces as an unhandled 500.
        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenReturn(false);

        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(WorkspaceErrorType.WORKSPACE_NOT_FOUND.getErrorKey());

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAddWorkspaceUserRejectsDuplicate() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
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
        // Removing a non-member throws NOT_MEMBER rather than answering false, so the caller can tell "nothing to
        // remove" from "removal succeeded" and the GraphQL layer surfaces a typed error instead of a success toast on
        // a stale members view.
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("not a member");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserBlockedWhenLastAdmin() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserDeletesAndEvictsCache() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();

        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserAllowedWhenAnotherAdminExists() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(2L);

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();
        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testUpdateWorkspaceUserRoleChangesRole() {
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
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

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
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

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN);

        verify(workspaceUserRepository, never())
            .countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleRejectsUnknownMembership() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
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
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Cannot demote your own role");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
        verify(workspaceUserRepository, never())
            .countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsAnotherAdminToDemoteYou() {
        // Caller != subject: the demotion proceeds (and last-admin guard fires on its own terms).
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
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
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceService.getWorkspaceName(WORKSPACE_ID)).thenReturn("Engineering");

        workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER);

        // Reusing rather than rejecting: a workspace admin should not have to know whether a colleague already signed
        // up. The provisioning call is skipped entirely, because one address has at most one account.
        verify(userInvitationService, never()).inviteUser(any(), any());
        verify(workspaceUserRepository, times(1)).save(any(WorkspaceUser.class));
    }

    @Test
    void testInviteWorkspaceUserNotifiesAnExistingAccount() {
        User existingUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("colleague@example.com")).thenReturn(Optional.of(existingUser));
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceService.getWorkspaceName(WORKSPACE_ID)).thenReturn("Engineering");

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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

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
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(
                WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("already a member");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testInviteWorkspaceUserRejectsBothRolesBeforeProvisioning() {
        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(
                WORKSPACE_ID, "newcomer@example.com", WorkspaceRole.EDITOR, 900L))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Exactly one");

        // Provisioning first and validating afterwards mailed a claim link and then rolled the account, and its
        // reset_key, out from under it. Nothing is even looked up here.
        verify(userService, never()).fetchUserByEmail(any());
        verify(userInvitationService, never()).inviteUser(any(), any());
    }

    @Test
    void testInviteWorkspaceUserRejectsNeitherRoleBeforeProvisioning() {
        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "newcomer@example.com", null, null))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Exactly one");

        verify(userInvitationService, never()).inviteUser(any(), any());
    }

    @Test
    void testInviteWorkspaceUserRejectsAnUnknownWorkspaceBeforeProvisioning() {
        // The tenant admin path: hasWorkspaceScopeInEveryEnvironment grants unconditionally for a tenant admin,
        // existing workspace or not, so nothing upstream of this method stops an unknown id from reaching the
        // membership insert and failing on the foreign key as an unhandled 500.
        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenReturn(false);

        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "newcomer@example.com", WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(WorkspaceErrorType.WORKSPACE_NOT_FOUND.getErrorKey());

        verify(userService, never()).fetchUserByEmail(any());
        verify(userInvitationService, never()).inviteUser(any(), any());
        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testInviteWorkspaceUserRejectsAnUnknownCustomRoleBeforeProvisioning() {
        when(customRoleRepository.findById(900L)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "newcomer@example.com", null, 900L))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("does not exist");

        verify(userInvitationService, never()).inviteUser(any(), any());
        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testRoleSelectionViolationAndUnknownRoleCarryDifferentErrorTypes() {
        // One key for three meanings left a client unable to tell "you sent both roles" from "that role is gone", and
        // the old name promised a per-workspace role model custom_role never had.
        ConfigurationException selectionException = (ConfigurationException) catchThrowable(
            () -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, null));

        when(customRoleRepository.findById(900L)).thenReturn(Optional.empty());

        ConfigurationException notFoundException = (ConfigurationException) catchThrowable(
            () -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, 900L));

        assertThat(selectionException.getErrorKey())
            .isEqualTo(WorkspaceUserErrorType.ROLE_SELECTION_INVALID.getErrorKey());
        assertThat(notFoundException.getErrorKey())
            .isEqualTo(WorkspaceUserErrorType.CUSTOM_ROLE_NOT_FOUND.getErrorKey());
        assertThat(selectionException.getErrorKey()).isNotEqualTo(notFoundException.getErrorKey());
    }

    @Test
    void testSetEnvironmentRoleRejectsAnUnknownCustomRoleWithTheNotFoundErrorType() {
        when(customRoleRepository.findById(900L)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, null, 900L))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                    .isEqualTo(WorkspaceUserErrorType.CUSTOM_ROLE_NOT_FOUND.getErrorKey());
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
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
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
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
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
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(List.of());
        when(userService.getUsersByAuthorityName("ROLE_ADMIN")).thenReturn(List.of(createUser(USER_ID)));

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("as a tenant admin");

        // "Not a member" describes the row, not the access — misleading for someone who demonstrably administers
        // the workspace and appears in its members view as an inherited entry.
    }

    @Test
    void testUpdateWorkspaceUserRoleBlocksSelfPromotion() {
        // WORKSPACE_MEMBER_MANAGE is what authorizes this method, so without this guard a custom role carrying nothing
        // else is silently equivalent to full workspace ADMIN: its holder simply promotes themselves.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Cannot raise your own role");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsTenantAdminToSelfPromote() {
        // Tenant admins keep the same exemption they hold from the self-demotion guard: they already administer every
        // workspace through isTenantAdmin(), so refusing them a workspace row grants nothing.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN);

        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleRefusesGrantingScopesTheCallerDoesNotHold() {
        // A member manager who is not an admin must not make anyone ADMIN, a second account of their own included.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN)).thenReturn(Set.of("WORKSPACE_MANAGE"));
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKSPACE_MANAGE", Environment.PRODUCTION))
            .thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("WORKSPACE_MANAGE");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testInviteWorkspaceUserRefusesARoleAboveTheCallerBeforeProvisioning() {
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN)).thenReturn(Set.of("WORKSPACE_MANAGE"));
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKSPACE_MANAGE", Environment.DEVELOPMENT))
            .thenReturn(false);

        assertThatThrownBy(
            () -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "alt@example.com", WorkspaceRole.ADMIN))
                .isInstanceOf(AccessDeniedException.class);

        verify(userInvitationService, never()).inviteUser(any(), any());
        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAssignCustomRoleRefusesARoleCarryingScopesTheCallerDoesNotHold() {
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Operator", Set.of("DEPLOYMENT_DELETE"))));
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_DELETE", Environment.STAGING))
            .thenReturn(false);

        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("DEPLOYMENT_DELETE");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testUpdateWorkspaceUserRoleAllowsPromotingSomebodyElse() {
        // The self-promotion guard is about the caller, not about promotion: a member manager's whole job is raising
        // other people, up to the scopes they hold themselves.
        WorkspaceUser workspaceUser = new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal());

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN);

        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testUpdateWorkspaceUserRoleBlocksAnySelfChangeFromACustomRole() {
        // A custom role has no rank on the built-in ladder, so neither direction can be shown to be safe: ADMIN would
        // be an escalation, and anything lower would drop the management scopes the custom role carries.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 900L)));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("while you hold a custom role");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testUpdateWorkspaceUserRoleRejectsAMemberInExplicitMode() {
        // No implicit row means no workspace-wide role to change. Rewriting one of their environment rows instead would
        // change a role the caller never named, and "not a member" would describe the missing row rather than the
        // access.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("per-environment roles");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAssignCustomRoleBlocksMovingYourselfOntoARoleWithoutMemberManage() {
        // The sibling updateWorkspaceUserRole refuses the same move expressed as a built-in demotion. Both paths reach
        // the same state — the caller without the scope that authorized the call — so both must refuse it.
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("does not grant WORKSPACE_MEMBER_MANAGE");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAssignCustomRoleBlocksMovingYourselfOntoARicherRoleThatKeepsMemberManage() {
        // Keeping WORKSPACE_MEMBER_MANAGE makes the move recoverable, not safe: this role carries member management
        // plus
        // every other scope, so allowing it would make WORKSPACE_MEMBER_MANAGE self-escalating by another name — the
        // holder of a role that grants nothing else simply hands themselves a richer one. A custom role has no rank on
        // the built-in ladder, so nothing can show that what it adds is something the caller already held.
        when(customRoleRepository.findById(900L))
            .thenReturn(
                Optional.of(
                    new CustomRole(
                        "Owner", Set.of("WORKSPACE_MEMBER_MANAGE", "CONNECTION_EDIT", "WORKFLOW_EDIT"))));
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("cannot be shown to be no more than you already hold");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testAssignCustomRoleAllowsTenantAdminToMoveThemselvesOntoAnyRole() {
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L);

        assertThat(result.getCustomRoleId()).isEqualTo(900L);
    }

    @Test
    void testAddWorkspaceUserRejectsAMemberWhoOnlyHoldsEnvironmentRows() {
        // The duplicate guard asks whether any row exists, not whether an implicit one does: writing an implicit row
        // beside their environment rows would put one member in both modes at once.
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already a member");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testRemoveWorkspaceUserDeletesEveryRowTheMemberHolds() {
        // The delete takes every row, so the read that guards it must too. A single-result read would have thrown
        // IncorrectResultSizeDataAccessException on this member rather than removing them.
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.DEVELOPMENT)));

        boolean result = workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID);

        assertThat(result).isTrue();

        verify(workspaceUserRepository, times(1)).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
        verify(permissionService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRemoveWorkspaceUserGuardsTheLastAdminHeldOnAnEnvironmentRow() {
        // The ADMIN row is deliberately not first: reading only the member's first or implicit row would walk past it
        // and delete the workspace's last admin.
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.DEVELOPMENT),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));
        when(workspaceUserRepository.countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
            .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testUpdateWorkspaceUserRoleReachesTheImplicitRowOfAMemberWhoAlsoHoldsEnvironmentRows() {
        // Whatever else the member holds, the workspace-wide write lands on the implicit row and asks no query that
        // could see more than one row.
        WorkspaceUser implicitWorkspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(implicitWorkspaceUser));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        assertThat(implicitWorkspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.VIEWER.ordinal());

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testInviteWorkspaceUserNamesTheWorkspaceWithoutSpendingWorkspaceView() {
        // A custom role carrying only WORKSPACE_MEMBER_MANAGE is authorized to make this invite. Reading the name
        // through the WORKSPACE_VIEW-gated getWorkspace made the invite fail for an address that already had an
        // account, after the membership had already been written.
        User existingUser = createUser(USER_ID);

        when(userService.fetchUserByEmail("colleague@example.com")).thenReturn(Optional.of(existingUser));
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(false);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceService.getWorkspaceName(WORKSPACE_ID)).thenReturn("Engineering");

        workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "colleague@example.com", WorkspaceRole.VIEWER);

        verify(userInvitationService).notifyAddedToWorkspace(existingUser, "Engineering");
        verify(workspaceService, never()).getWorkspace(WORKSPACE_ID);
    }

    @Test
    void testIsWorkspaceMemberHoldsForAMemberInExplicitMode() {
        // Membership is "any row", not "an implicit row": someone granted only Production is no less a member, and a
        // connection may be shared with them.
        when(workspaceUserRepository.existsByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(true);

        assertThat(workspaceUserService.isWorkspaceMember(USER_ID, WORKSPACE_ID)).isTrue();
    }

    @Test
    void testRemoveWorkspaceUserRefusesTheOnlyAdminHoldingAnAdminRowPerEnvironment() {
        // The workspace's only admin, in explicit mode with ADMIN in all three environments -- which is what the
        // hasWorkspaceScopeInEveryEnvironment gate on this method requires of them. Counted as rows they read as three
        // admins, the guard passes, the delete takes all three and the workspace is left with nobody who can administer
        // it. The guard must therefore count distinct members; that this query does so against a real database is
        // PerEnvironmentRoleIntTest's job, since a mock can only be told what to return.
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.DEVELOPMENT),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.STAGING),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));
        when(
            workspaceUserRepository.countDistinctMembersWithWorkspaceRole(
                WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal()))
                    .thenReturn(1L);

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last admin");

        verify(workspaceUserRepository, never()).deleteByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);

        // Which query the guard asks is the whole point, and a mock can only be told what to answer — so pin the ask.
        verify(workspaceUserRepository, times(1))
            .countDistinctMembersWithWorkspaceRole(WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testSetEnvironmentRoleBlocksSelfPromotionInThatEnvironment() {
        // Naming one environment must not be the way around the workspace-wide self-promotion guard. This gate asks
        // only
        // for WORKSPACE_MEMBER_MANAGE in the environment being written, so without this check its holder grants
        // themselves ADMIN of Production.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.ADMIN, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Cannot raise your own role in environment PRODUCTION");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testSetEnvironmentRoleBlocksSelfPromotionByACustomRoleHolder() {
        // The exact escalation the review found: a member whose custom role carries only WORKSPACE_MEMBER_MANAGE. The
        // environment guards see nothing, because resolveAdminEnvironments reads ADMIN rows and a custom-role row is
        // not
        // one, so both validateNotSelfDemotionFromEnvironments and validateEnvironmentsKeepAnAdmin no-op.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 900L)));

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.ADMIN, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Cannot raise your own role in environment PRODUCTION");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testSetEnvironmentRoleBlocksMovingYourselfOntoACustomRoleThere() {
        // Symmetric to the workspace-wide path: a custom role has no rank on the built-in ladder, so neither direction
        // can be shown to be safe and the answer is "ask another admin".
        when(customRoleRepository.findById(900L))
            .thenReturn(Optional.of(new CustomRole("Deployer", Set.of("WORKSPACE_MEMBER_MANAGE"))));
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, null, 900L))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("onto a custom role");

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testSetEnvironmentRoleAllowsLoweringYourOwnRoleThere() {
        // The guard is about raising, not about touching your own row: narrowing your own reach takes nothing away from
        // anybody else and cannot be an escalation.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.VIEWER, null);

        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.VIEWER.ordinal());
        assertThat(result.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testSetEnvironmentRoleAllowsTenantAdminToSelfPromoteInThatEnvironment() {
        // The same exemption the other self-guards grant: a tenant admin already administers every workspace through
        // isTenantAdmin(), so refusing them a row grants nothing.
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.ADMIN, null);

        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testSetEnvironmentRoleAllowsPromotingSomebodyElseThere() {
        // Caller-scoped, like every other self-guard: granting other people roles is a member manager's whole job, up
        // to the scopes they hold there.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(false);
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceUser result = workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.ADMIN, null);

        assertThat(result.getWorkspaceRole()).isEqualTo(WorkspaceRole.ADMIN.ordinal());
    }

    @Test
    void testSetEnvironmentRoleRefusesGrantingScopesTheCallerDoesNotHoldThere() {
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN)).thenReturn(Set.of("WORKSPACE_MANAGE"));
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKSPACE_MANAGE", Environment.PRODUCTION))
            .thenReturn(false);

        assertThatThrownBy(() -> workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.ADMIN, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testSetEnvironmentRoleRequiresEveryEnvironmentToReplaceAWorkspaceWideRole() {
        // The write removes the member's workspace-wide role, so it withdraws their access in every other environment
        // as well; member management in one environment is not enough to do that.
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .thenReturn(false);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR)));

        assertThatThrownBy(() -> workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.VIEWER, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("every environment");

        verify(workspaceUserRepository, never()).delete(any(WorkspaceUser.class));
        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }
}

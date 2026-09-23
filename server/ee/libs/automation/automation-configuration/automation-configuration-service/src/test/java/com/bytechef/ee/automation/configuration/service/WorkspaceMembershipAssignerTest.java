/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner.WorkspaceAssignment;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

/**
 * Covers the platform-to-automation seam that places an invited user into workspaces.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceMembershipAssignerTest {

    private static final long USER_ID = 42L;

    private PermissionService permissionService;
    private WorkspaceService workspaceService;
    private WorkspaceUserAuditPublisher workspaceUserAuditPublisher;
    private WorkspaceUserRepository workspaceUserRepository;
    private WorkspaceUserService workspaceUserService;
    private WorkspaceMembershipAssignerImpl workspaceMembershipAssigner;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        workspaceService = mock(WorkspaceService.class);
        workspaceUserAuditPublisher = mock(WorkspaceUserAuditPublisher.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);
        workspaceUserService = mock(WorkspaceUserService.class);

        workspaceMembershipAssigner = new WorkspaceMembershipAssignerImpl(
            permissionService, workspaceService, workspaceUserAuditPublisher, workspaceUserRepository,
            workspaceUserService);
    }

    @Test
    void testAssignDelegatesEachAssignment() {
        workspaceMembershipAssigner.assign(
            USER_ID, List.of(new WorkspaceAssignment(1L, "EDITOR"), new WorkspaceAssignment(2L, "VIEWER")));

        // Delegating rather than writing rows keeps the already-a-member guard and the scope-cache eviction in one
        // place, so an invited member and a hand-added one go through identical checks.
        verify(workspaceUserService).addWorkspaceUser(USER_ID, 1L, WorkspaceRole.EDITOR);
        verify(workspaceUserService).addWorkspaceUser(USER_ID, 2L, WorkspaceRole.VIEWER);
    }

    @Test
    void testAssignWithNoAssignmentsDoesNothing() {
        workspaceMembershipAssigner.assign(USER_ID, List.of());

        verifyNoInteractions(workspaceUserService);
    }

    @Test
    void testAssignRejectsUnknownRole() {
        assertThatThrownBy(
            () -> workspaceMembershipAssigner.assign(USER_ID, List.of(new WorkspaceAssignment(1L, "SUPERUSER"))))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unknown workspace role");

        // Substituting a default role would hand out an access level nobody asked for, so nothing is written.
        verify(workspaceUserService, never()).addWorkspaceUser(
            ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.any());
    }

    @Test
    void testValidateAssignmentsRejectsUnknownRoleWithoutTouchingMembership() {
        assertThatThrownBy(
            () -> workspaceMembershipAssigner.validateAssignments(List.of(new WorkspaceAssignment(1L, "SUPERUSER"))))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unknown workspace role");

        // The whole point of the pre-check: the caller runs it before an account exists, so it must not write.
        verifyNoInteractions(workspaceUserService);
    }

    @Test
    void testValidateAssignmentsRejectsUnknownWorkspace() {
        when(workspaceService.workspaceExists(1L)).thenReturn(false);

        assertThatThrownBy(
            () -> workspaceMembershipAssigner.validateAssignments(List.of(new WorkspaceAssignment(1L, "EDITOR"))))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("does not exist");

        // Left to the insert, an unknown workspace fails on the foreign key, which reads as a 500 rather than as the
        // caller error it is.
        verifyNoInteractions(workspaceUserService);
    }

    @Test
    void testValidateAssignmentsAcceptsAKnownWorkspaceAndRole() {
        when(workspaceService.workspaceExists(1L)).thenReturn(true);

        assertThatCode(
            () -> workspaceMembershipAssigner.validateAssignments(List.of(new WorkspaceAssignment(1L, "EDITOR"))))
                .doesNotThrowAnyException();
    }

    @Test
    void testRemoveMembershipsDeletesEveryRowTheUserHolds() {
        workspaceMembershipAssigner.removeMemberships(USER_ID);

        // Not through removeWorkspaceUser: that refuses the workspace's last admin and is gated per workspace, neither
        // of which fits an account that is going away regardless.
        verify(workspaceUserRepository).deleteByUserId(USER_ID);
        verifyNoInteractions(workspaceUserService);
    }

    @Test
    void testRemoveMembershipsEvictsTheScopeCacheForEveryWorkspaceTheUserHeld() {
        // A deleted user's (userId, workspaceId) scope cache entry would otherwise outlive the account for the
        // cache's TTL -- removeWorkspaceUser evicts on every operator-driven removal, and this path must match it.
        when(workspaceUserRepository.findAllByUserId(USER_ID)).thenReturn(
            List.of(
                WorkspaceUser.forRole(USER_ID, 1L, WorkspaceRole.EDITOR),
                WorkspaceUser.forRole(USER_ID, 2L, WorkspaceRole.VIEWER)));

        workspaceMembershipAssigner.removeMemberships(USER_ID);

        verify(permissionService).evictWorkspaceScopeCaches(
            List.of(new UserWorkspacePair(USER_ID, 1L), new UserWorkspacePair(USER_ID, 2L)));
    }

    @Test
    void testRemoveMembershipsDedupsCacheEvictionAcrossEnvironmentRows() {
        // A member in explicit mode holds one row per environment. Evicting once per row rather than once per
        // workspace would be redundant, not wrong, but the pair list is exactly what a reader would check first.
        when(workspaceUserRepository.findAllByUserId(USER_ID)).thenReturn(
            List.of(
                WorkspaceUser.forRole(USER_ID, 1L, WorkspaceRole.VIEWER, Environment.DEVELOPMENT),
                WorkspaceUser.forRole(USER_ID, 1L, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        workspaceMembershipAssigner.removeMemberships(USER_ID);

        verify(permissionService).evictWorkspaceScopeCaches(List.of(new UserWorkspacePair(USER_ID, 1L)));
    }

    @Test
    void testRemoveMembershipsPublishesOneAuditEventPerWorkspace() {
        // removeWorkspaceUser publishes WORKSPACE_USER_REMOVED on every operator-driven removal. A user delete taking
        // the same rows out from under them must leave the same trace, or the audit log reads as though those
        // memberships never ended.
        when(workspaceUserRepository.findAllByUserId(USER_ID)).thenReturn(
            List.of(
                WorkspaceUser.forRole(USER_ID, 1L, WorkspaceRole.EDITOR),
                WorkspaceUser.forRole(USER_ID, 2L, WorkspaceRole.VIEWER)));

        workspaceMembershipAssigner.removeMemberships(USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(workspaceUserAuditPublisher, times(2))
            .publish(eq(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED), dataCaptor.capture());

        assertThat(dataCaptor.getAllValues())
            .extracting(data -> data.get("workspaceId"), data -> data.get("userId"))
            .containsExactlyInAnyOrder(Tuple.tuple("1", "42"), Tuple.tuple("2", "42"));
    }

    @Test
    void testRemoveMembershipsWithNoRowsEvictsNothingAndPublishesNothing() {
        when(workspaceUserRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        workspaceMembershipAssigner.removeMemberships(USER_ID);

        verify(permissionService).evictWorkspaceScopeCaches(List.of());
        verifyNoInteractions(workspaceUserAuditPublisher);
    }
}

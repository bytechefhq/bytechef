/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserServiceTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final WorkspaceUserAuditPublisher workspaceUserAuditPublisher = mock(WorkspaceUserAuditPublisher.class);
    private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);
    private final WorkspaceUserService workspaceUserService = new WorkspaceUserServiceImpl(
        permissionService, workspaceUserAuditPublisher, workspaceUserRepository);

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteUserWorkspaceUsersRemovesEveryMembershipAndEvictsItsScopes() {
        WorkspaceUser adminMembership = WorkspaceUser.forRole(7L, 1L, WorkspaceRole.ADMIN);
        WorkspaceUser editorMembership = WorkspaceUser.forRole(7L, 2L, WorkspaceRole.EDITOR);
        List<WorkspaceUser> workspaceUsers = List.of(adminMembership, editorMembership);

        when(workspaceUserRepository.findAllByUserId(7L)).thenReturn(workspaceUsers);

        workspaceUserService.deleteUserWorkspaceUsers(7L);

        verify(workspaceUserRepository).deleteAll(workspaceUsers);

        ArgumentCaptor<Collection<UserWorkspacePair>> userWorkspacePairsArgumentCaptor =
            ArgumentCaptor.forClass(Collection.class);

        verify(permissionService).evictWorkspaceScopeCaches(userWorkspacePairsArgumentCaptor.capture());

        assertThat(userWorkspacePairsArgumentCaptor.getValue()).containsExactlyInAnyOrder(
            new UserWorkspacePair(7L, 1L), new UserWorkspacePair(7L, 2L));

        verify(workspaceUserAuditPublisher, times(2)).publish(
            eq(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED), anyMap());
    }

    @Test
    void testDeleteUserWorkspaceUsersWithoutMembershipsDoesNothing() {
        when(workspaceUserRepository.findAllByUserId(7L)).thenReturn(List.of());

        workspaceUserService.deleteUserWorkspaceUsers(7L);

        verify(workspaceUserRepository, never()).deleteAll(any());
        verify(permissionService, never()).evictWorkspaceScopeCaches(any());
        verify(workspaceUserAuditPublisher, never()).publish(any(), anyMap());
    }
}

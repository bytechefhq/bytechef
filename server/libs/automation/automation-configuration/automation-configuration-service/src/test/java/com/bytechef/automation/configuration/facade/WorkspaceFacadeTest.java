/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link WorkspaceFacadeImpl#getUserWorkspaces(long)} orchestrates {@link WorkspaceService} and
 * {@link PermissionService} correctly: tenant admins see every workspace, while a regular user only sees the workspaces
 * they hold a role in.
 *
 * @author Ivica Cardic
 */
class WorkspaceFacadeTest {

    private static final long USER_ID = 42L;

    private static final Workspace WORKSPACE_1 = new Workspace(1L, "Workspace 1");
    private static final Workspace WORKSPACE_2 = new Workspace(2L, "Workspace 2");
    private static final Workspace WORKSPACE_3 = new Workspace(3L, "Workspace 3");

    private PermissionService permissionService;
    private WorkspaceService workspaceService;
    private WorkspaceFacadeImpl workspaceFacade;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        workspaceService = mock(WorkspaceService.class);

        workspaceFacade = new WorkspaceFacadeImpl(permissionService, workspaceService);

        when(workspaceService.getWorkspaces()).thenReturn(List.of(WORKSPACE_1, WORKSPACE_2, WORKSPACE_3));
    }

    @Test
    void testGetUserWorkspacesReturnsAllForTenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        List<Workspace> workspaces = workspaceFacade.getUserWorkspaces(USER_ID);

        assertThat(workspaces).containsExactly(WORKSPACE_1, WORKSPACE_2, WORKSPACE_3);

        verify(permissionService, never()).getMyWorkspaceRole(anyLong());
    }

    @Test
    void testGetUserWorkspacesFiltersToMembershipForNonAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.getMyWorkspaceRole(WORKSPACE_1.getId())).thenReturn("VIEWER");
        when(permissionService.getMyWorkspaceRole(WORKSPACE_2.getId())).thenReturn(null);
        when(permissionService.getMyWorkspaceRole(WORKSPACE_3.getId())).thenReturn("EDITOR");

        List<Workspace> workspaces = workspaceFacade.getUserWorkspaces(USER_ID);

        assertThat(workspaces).containsExactly(WORKSPACE_1, WORKSPACE_3);
    }

    @Test
    void testGetUserWorkspacesEmptyWhenNonAdminHasNoMemberships() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.getMyWorkspaceRole(anyLong())).thenReturn(null);

        assertThat(workspaceFacade.getUserWorkspaces(USER_ID)).isEmpty();
    }
}

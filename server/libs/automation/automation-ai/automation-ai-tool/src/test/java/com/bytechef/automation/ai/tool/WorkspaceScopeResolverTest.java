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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class WorkspaceScopeResolverTest {

    private static final long USER_ID = 1L;

    private final UserService userService = mock(UserService.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
    private final WorkspaceService workspaceService = mock(WorkspaceService.class);
    private final WorkspaceScopeResolver workspaceScopeResolver =
        new WorkspaceScopeResolver(userService, workspaceFacade, workspaceService);

    @BeforeEach
    void beforeEach() {
        User user = new User();

        user.setId(USER_ID);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));
        when(workspaceService.getWorkspaces()).thenReturn(
            List.of(workspace(7L, "Mine"), workspace(8L, "Foreign"), workspace(9L, "Other foreign")));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testExplicitForeignWorkspaceIdIsRejected() {
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(7L, "Mine")));

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(8L, null);

        assertThat(workspaceScope).isInstanceOf(WorkspaceScopeResolver.Rejected.class);
        assertThat(((WorkspaceScopeResolver.Rejected) workspaceScope).response())
            .contains("error")
            .contains("not accessible")
            .doesNotContain("Foreign");
    }

    @Test
    void testExplicitNonexistentWorkspaceIdIsRejectedWithTheSameMessage() {
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(7L, "Mine")));

        WorkspaceScopeResolver.Rejected foreign = (WorkspaceScopeResolver.Rejected) workspaceScopeResolver.resolve(
            8L, null);
        WorkspaceScopeResolver.Rejected nonexistent =
            (WorkspaceScopeResolver.Rejected) workspaceScopeResolver.resolve(12345L, null);

        assertThat(nonexistent.response()
            .replace("12345", "8")).isEqualTo(foreign.response());
    }

    @Test
    void testExplicitOwnWorkspaceIdIsResolved() {
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(
            List.of(workspace(7L, "Mine"), workspace(10L, "Also mine")));

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(10L, "production");

        assertThat(workspaceScope).isEqualTo(new WorkspaceScopeResolver.Resolved(10L, 2L));
    }

    @Test
    void testAutoSelectUsesTheUsersOnlyWorkspaceNotTheTenantList() {
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(7L, "Mine")));

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(null, null);

        assertThat(workspaceScope).isEqualTo(new WorkspaceScopeResolver.Resolved(7L, 0L));
        verify(workspaceService, never()).getWorkspaces();
    }

    @Test
    void testWorkspaceRequiredListsOnlyTheUsersWorkspaces() {
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(
            List.of(workspace(7L, "Mine"), workspace(10L, "Also mine")));

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(null, null);

        assertThat(workspaceScope).isInstanceOf(WorkspaceScopeResolver.Rejected.class);
        assertThat(((WorkspaceScopeResolver.Rejected) workspaceScope).response())
            .contains("workspace_required")
            .contains("Mine")
            .contains("Also mine")
            .doesNotContain("Foreign");
    }

    @Test
    void testMissingUserIsRejected() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.empty());

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(7L, null);

        assertThat(workspaceScope).isInstanceOf(WorkspaceScopeResolver.Rejected.class);
        verify(workspaceService, never()).getWorkspaces();
    }

    @Test
    void testAnonymousManagementPrincipalKeepsTheTenantWideList() {
        SecurityContextHolder.getContext()
            .setAuthentication(McpAnonymousAuthenticationToken.ofManagementMcpServer());

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(null, null);

        assertThat(((WorkspaceScopeResolver.Rejected) workspaceScope).response())
            .contains("workspace_required")
            .contains("Mine")
            .contains("Foreign")
            .contains("Other foreign");
        assertThat(workspaceScopeResolver.resolve(8L, null))
            .isEqualTo(new WorkspaceScopeResolver.Resolved(8L, 0L));
        verify(workspaceFacade, never()).getUserWorkspaces(anyLong());
        verify(userService, never()).fetchCurrentUser();
    }

    @Test
    void testAnonymousAutomationPrincipalDoesNotGetTheTenantWideList() {
        SecurityContextHolder.getContext()
            .setAuthentication(McpAnonymousAuthenticationToken.ofAutomationMcpServer(3L));

        when(userService.fetchCurrentUser()).thenReturn(Optional.empty());

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(null, null);

        assertThat(((WorkspaceScopeResolver.Rejected) workspaceScope).response()).doesNotContain("Foreign");
        verify(workspaceService, never()).getWorkspaces();
    }

    @Test
    void testUnknownEnvironmentIsRejected() {
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(7L, "Mine")));

        WorkspaceScopeResolver.WorkspaceScope workspaceScope = workspaceScopeResolver.resolve(7L, "NOPE");

        assertThat(((WorkspaceScopeResolver.Rejected) workspaceScope).response())
            .contains("Unknown environment")
            .contains("NOPE");
    }

    private static Workspace workspace(long id, String name) {
        Workspace workspace = new Workspace();

        workspace.setId(id);
        workspace.setName(name);

        return workspace;
    }
}

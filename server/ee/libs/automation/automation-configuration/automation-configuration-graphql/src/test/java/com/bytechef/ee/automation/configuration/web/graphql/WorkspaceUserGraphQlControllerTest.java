/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.automation.configuration.web.graphql.WorkspaceUserGraphQlController.WorkspaceUserInfo;
import com.bytechef.ee.automation.configuration.web.graphql.WorkspaceUserGraphQlController.WorkspaceUserView;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Covers the membership view's union of stored rows with tenant admins projected as inherited workspace admins.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserGraphQlControllerTest {

    private static final long WORKSPACE_ID = 7L;

    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserService userService = mock(UserService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);
    private final WorkspaceUserGraphQlController controller =
        new WorkspaceUserGraphQlController(permissionService, userService, workspaceUserService);

    @Test
    void testMyWorkspaceScopesWithAnEnvironmentAsksForThatEnvironment() {
        when(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(controller.myWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION))
            .containsExactly("WORKFLOW_VIEW");

        verify(permissionService, never()).getMyWorkspaceScopes(anyLong());
    }

    @Test
    void testMyWorkspaceScopesWithoutAnEnvironmentKeepsTheWorkspaceWideAnswer() {
        when(permissionService.getMyWorkspaceScopes(WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_EDIT", "WORKFLOW_VIEW"));

        assertThat(controller.myWorkspaceScopes(WORKSPACE_ID, null))
            .containsExactlyInAnyOrder("WORKFLOW_EDIT", "WORKFLOW_VIEW");

        verify(permissionService, never()).getMyWorkspaceScopes(anyLong(), any(Environment.class));
    }

    @Test
    void testWorkspaceUsersProjectsTenantAdminsAsInherited() {
        when(workspaceUserService.getWorkspaceWorkspaceUsers(WORKSPACE_ID))
            .thenReturn(List.of(workspaceUser(1L, WorkspaceRole.EDITOR)));
        when(userService.getUsersByAuthorityName(AuthorityConstants.ADMIN)).thenReturn(List.of(user(99L)));

        List<WorkspaceUserView> views = controller.workspaceUsers(WORKSPACE_ID);

        // A tenant admin administers every workspace but holds no row. Omitting them showed fewer people than could
        // actually administer the workspace, which is the defect this closes.
        assertThat(views).hasSize(2);

        WorkspaceUserView inherited = views.stream()
            .filter(WorkspaceUserView::inherited)
            .findFirst()
            .orElseThrow();

        assertThat(inherited.userId()).isEqualTo(99L);
        assertThat(inherited.workspaceRole()).isEqualTo(WorkspaceRole.ADMIN.name());

        // No row backs an inherited entry, so it carries no id — that absence is what the client keys on to lock
        // the row's controls.
        assertThat(inherited.id()).isNull();
    }

    @Test
    void testWorkspaceUsersShowsAStoredRoleForATenantAdminWhoIsAlsoAMember() {
        when(workspaceUserService.getWorkspaceWorkspaceUsers(WORKSPACE_ID))
            .thenReturn(List.of(workspaceUser(99L, WorkspaceRole.VIEWER)));
        when(userService.getUsersByAuthorityName(AuthorityConstants.ADMIN)).thenReturn(List.of(user(99L)));

        List<WorkspaceUserView> views = controller.workspaceUsers(WORKSPACE_ID);

        // Once, not twice — and as VIEWER, because that is the role the authorization path would use if they lost
        // tenant admin. Showing ADMIN here would misreport the durable state of their access.
        assertThat(views).hasSize(1);
        assertThat(views.getFirst()
            .inherited()).isFalse();
        assertThat(views.getFirst()
            .workspaceRole()).isEqualTo(WorkspaceRole.VIEWER.name());
    }

    @Test
    void testWorkspaceUsersWithNoTenantAdminsReturnsOnlyStoredRows() {
        when(workspaceUserService.getWorkspaceWorkspaceUsers(WORKSPACE_ID))
            .thenReturn(List.of(workspaceUser(1L, WorkspaceRole.EDITOR)));
        when(userService.getUsersByAuthorityName(AuthorityConstants.ADMIN)).thenReturn(List.of());

        List<WorkspaceUserView> views = controller.workspaceUsers(WORKSPACE_ID);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst()
            .inherited()).isFalse();
    }

    private static WorkspaceUser workspaceUser(long userId, WorkspaceRole workspaceRole) {
        return new WorkspaceUser(userId, WORKSPACE_ID, workspaceRole.ordinal());
    }

    private static User user(long id) {
        User user = new User();

        user.setId(id);

        return user;
    }

    @Test
    void testUserResolvesToTheAccountBehindTheRow() {
        User user = user(1L);

        user.setEmail("member@example.com");
        user.setFirstName("Mem");
        user.setLastName("Ber");

        when(userService.fetchUser(1L)).thenReturn(Optional.of(user));

        WorkspaceUserInfo info = controller.user(WorkspaceUserView.stored(workspaceUser(1L, WorkspaceRole.EDITOR)));

        assertThat(info).isNotNull();
        assertThat(info.email()).isEqualTo("member@example.com");
    }

    @Test
    void testUserResolvesToNullForAnOrphanedMembershipRow() {
        when(userService.fetchUser(1L)).thenReturn(Optional.empty());

        // getUser threw UserNotFoundException, and this fetcher runs once per row: a single row whose user is gone
        // turned the whole members page into an INTERNAL_ERROR for every viewer. The workspace_user.user_id foreign key
        // exists only in the mono Liquibase context, so nothing at the schema level rules such a row out elsewhere.
        WorkspaceUserInfo info = controller.user(WorkspaceUserView.stored(workspaceUser(1L, WorkspaceRole.EDITOR)));

        assertThat(info).isNull();
    }

    @Test
    void testStoredViewRendersNoRoleForAnOrdinalOutsideTheEnum() {
        WorkspaceUser corruptedWorkspaceUser = mock(WorkspaceUser.class);

        when(corruptedWorkspaceUser.getWorkspaceRole()).thenReturn(99);

        // Spring Data JDBC hydrates workspace_role straight into the field and bypasses the constructor's range check,
        // so a value the enum has no member for reaches this mapper. Indexing values() let one such row take the whole
        // members page down with an ArrayIndexOutOfBoundsException for every viewer; the field is nullable in the
        // schema, so one entry without a role is the better failure.
        WorkspaceUserView view = WorkspaceUserView.stored(corruptedWorkspaceUser);

        assertThat(view.workspaceRole()).isNull();
    }

    @Test
    void testOneOrphanedRowDoesNotStopTheOtherRowsResolving() {
        User survivor = user(2L);

        survivor.setEmail("survivor@example.com");

        when(userService.fetchUser(1L)).thenReturn(Optional.empty());
        when(userService.fetchUser(2L)).thenReturn(Optional.of(survivor));

        WorkspaceUserInfo orphan = controller.user(WorkspaceUserView.stored(workspaceUser(1L, WorkspaceRole.EDITOR)));
        WorkspaceUserInfo resolved = controller.user(WorkspaceUserView.stored(workspaceUser(2L, WorkspaceRole.VIEWER)));

        assertThat(orphan).isNull();
        assertThat(resolved).isNotNull();
        assertThat(resolved.email()).isEqualTo("survivor@example.com");
    }
}

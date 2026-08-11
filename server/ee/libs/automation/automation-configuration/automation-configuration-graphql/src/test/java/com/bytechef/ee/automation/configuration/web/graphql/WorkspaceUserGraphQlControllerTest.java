/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.automation.configuration.web.graphql.WorkspaceUserGraphQlController.WorkspaceUserView;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
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
}

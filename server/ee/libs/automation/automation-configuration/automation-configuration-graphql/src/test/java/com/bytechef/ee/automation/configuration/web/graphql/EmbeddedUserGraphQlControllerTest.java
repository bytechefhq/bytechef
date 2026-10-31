/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.facade.EmbeddedUserFacade;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.web.graphql.EmbeddedUserGraphQlController.EmbeddedUserView;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedUserGraphQlControllerTest {

    private final EmbeddedUserFacade embeddedUserFacade = mock(EmbeddedUserFacade.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserService userService = mock(UserService.class);
    private final EmbeddedUserGraphQlController controller =
        new EmbeddedUserGraphQlController(embeddedUserFacade, permissionService, userService);

    @Test
    void testEmbeddedUsersListsMembersAndInheritedTenantAdmins() {
        when(embeddedUserFacade.getEmbeddedUsers())
            .thenReturn(List.of(EmbeddedUser.forRole(1L, WorkspaceRole.EDITOR)));
        when(userService.getUsersByAuthorityName(AuthorityConstants.ADMIN))
            .thenReturn(List.of(user(1L), user(99L)));

        List<EmbeddedUserView> views = controller.embeddedUsers();

        assertThat(views).hasSize(2);

        EmbeddedUserView stored = views.getFirst();

        assertThat(stored.inherited()).isFalse();
        assertThat(stored.embeddedRole()).isEqualTo(WorkspaceRole.EDITOR.name());

        EmbeddedUserView inherited = views.getLast();

        assertThat(inherited.inherited()).isTrue();
        assertThat(inherited.userId()).isEqualTo(99L);
        assertThat(inherited.embeddedRole()).isEqualTo(WorkspaceRole.ADMIN.name());
        assertThat(inherited.id()).isNull();
    }

    @Test
    void testMyEmbeddedScopesPassesTheEnvironmentThrough() {
        when(permissionService.getMyEmbeddedScopes(Environment.PRODUCTION)).thenReturn(Set.of("WORKFLOW_VIEW"));
        when(permissionService.getMyEmbeddedScopes(null)).thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(controller.myEmbeddedScopes(Environment.PRODUCTION)).containsExactly("WORKFLOW_VIEW");
        assertThat(controller.myEmbeddedScopes(null)).containsExactly("WORKFLOW_EDIT");

        verify(permissionService).getMyEmbeddedScopes(Environment.PRODUCTION);
        verify(permissionService).getMyEmbeddedScopes(null);
    }

    @Test
    void testAddEmbeddedUserMapsTheRole() {
        when(embeddedUserFacade.addEmbeddedUser(5L, WorkspaceRole.VIEWER))
            .thenReturn(EmbeddedUser.forRole(5L, WorkspaceRole.VIEWER));

        EmbeddedUserView view = controller.addEmbeddedUser(5L, WorkspaceRole.VIEWER);

        assertThat(view.userId()).isEqualTo(5L);
        assertThat(view.embeddedRole()).isEqualTo(WorkspaceRole.VIEWER.name());
        assertThat(view.inherited()).isFalse();
    }

    @Test
    void testAddEmbeddedUserByEmailMapsTheRole() {
        when(embeddedUserFacade.addEmbeddedUserByEmail("jane@example.com", WorkspaceRole.EDITOR))
            .thenReturn(EmbeddedUser.forRole(6L, WorkspaceRole.EDITOR));

        EmbeddedUserView view = controller.addEmbeddedUserByEmail("jane@example.com", WorkspaceRole.EDITOR);

        assertThat(view.userId()).isEqualTo(6L);
        assertThat(view.embeddedRole()).isEqualTo(WorkspaceRole.EDITOR.name());
        assertThat(view.inherited()).isFalse();
    }

    private static User user(long id) {
        User user = new User();

        user.setId(id);

        return user;
    }
}

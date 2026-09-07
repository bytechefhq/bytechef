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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * The {@code 'Connection'} token is only a check because this resolver is registered for it — both
 * {@code PermissionService.isResourceOwner} and {@code hasResourceRole} deny every non-tenant-admin when the registry
 * lookup misses, so the four connection-sharing mutations that name the token were a lockout without it.
 * <p>
 * Both coordinates are asserted because the two disjuncts of those gates read different ones: the owner branch reads
 * {@code ownerUserId}, the workspace-ADMIN branch reads {@code workspaceId}. A resolver that filled only one would fix
 * only one half of the gate.
 *
 * @author Ivica Cardic
 */
class ConnectionOwnershipResolverTest {

    private static final long CONNECTION_ID = 11L;
    private static final String OWNER_LOGIN = "ivica";
    private static final long OWNER_USER_ID = 7L;
    private static final long WORKSPACE_ID = 9L;

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final UserService userService = mock(UserService.class);
    private final WorkspaceConnectionRepository workspaceConnectionRepository =
        mock(WorkspaceConnectionRepository.class);
    private final ConnectionOwnershipResolver resolver =
        new ConnectionOwnershipResolver(connectionService, userService, workspaceConnectionRepository);

    @Test
    void testResourceTypeMatchesTheTokenTheSharingGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("Connection");
    }

    @Test
    void testResolveOwnerReturnsBothTheOwningWorkspaceAndTheCreatingUser() {
        givenWorkspaceConnection();
        givenConnectionCreatedBy(OWNER_LOGIN);
        givenUser(OWNER_LOGIN, OWNER_USER_ID);

        assertThat(resolver.resolveOwner(CONNECTION_ID))
            .isEqualTo(ResourceOwner.of(OptionalLong.of(WORKSPACE_ID), OptionalLong.of(OWNER_USER_ID)));
    }

    @Test
    void testResolveOwnerFailsClosedForAnUnknownConnection() {
        givenWorkspaceConnection();

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(CONNECTION_ID))
            .as("a connection row that is gone must not resolve to an owner")
            .isEqualTo(ResourceOwner.of(OptionalLong.of(WORKSPACE_ID), OptionalLong.empty()));
    }

    @Test
    void testResolveOwnerFailsClosedForAConnectionInNoWorkspace() {
        when(workspaceConnectionRepository.findByConnectionId(CONNECTION_ID)).thenReturn(Optional.empty());

        givenConnectionCreatedBy(OWNER_LOGIN);
        givenUser(OWNER_LOGIN, OWNER_USER_ID);

        assertThat(resolver.resolveOwner(CONNECTION_ID))
            .as("an embedded or organization connection has no workspace_connection row, so no workspace role applies")
            .isEqualTo(ResourceOwner.of(OptionalLong.empty(), OptionalLong.of(OWNER_USER_ID)));
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheCreatingLoginMatchesNoUser() {
        givenWorkspaceConnection();
        givenConnectionCreatedBy("deleted-user");

        when(userService.fetchUserByLogin("deleted-user")).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(CONNECTION_ID))
            .isEqualTo(ResourceOwner.of(OptionalLong.of(WORKSPACE_ID), OptionalLong.empty()));
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheConnectionCarriesNoCreator() {
        givenWorkspaceConnection();
        givenConnectionCreatedBy(null);

        assertThat(resolver.resolveOwner(CONNECTION_ID))
            .isEqualTo(ResourceOwner.of(OptionalLong.of(WORKSPACE_ID), OptionalLong.empty()));
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    private void givenConnectionCreatedBy(String login) {
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setCreatedBy(login);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));
    }

    private void givenUser(String login, long userId) {
        User user = new User();

        user.setId(userId);

        when(userService.fetchUserByLogin(login)).thenReturn(Optional.of(user));
    }

    private void givenWorkspaceConnection() {
        when(workspaceConnectionRepository.findByConnectionId(CONNECTION_ID))
            .thenReturn(Optional.of(new WorkspaceConnection(CONNECTION_ID, WORKSPACE_ID)));
    }
}

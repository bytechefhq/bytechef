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
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConnectionOwnershipResolverTest {

    private final WorkspaceConnectionRepository workspaceConnectionRepository =
        mock(WorkspaceConnectionRepository.class);
    private final UserService userService = mock(UserService.class);

    private final ConnectionOwnershipResolver resolver =
        new ConnectionOwnershipResolver(workspaceConnectionRepository, userService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("Connection");
    }

    @Test
    void testResolvesWorkspaceAndOwner() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getWorkspaceId()).thenReturn(42L);
        when(workspaceConnection.getCreatedBy()).thenReturn("alice");
        when(workspaceConnectionRepository.findByConnectionId(1L)).thenReturn(Optional.of(workspaceConnection));

        User user = new User();

        user.setId(7L);

        when(userService.fetchUserByLogin("alice")).thenReturn(Optional.of(user));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
        assertThat(resolver.resolveOwner(1L)
            .ownerUserId()).hasValue(7L);
    }

    @Test
    void testUnknownConnectionIsUnknown() {
        when(workspaceConnectionRepository.findByConnectionId(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner(99L)
            .ownerUserId()).isEmpty();
    }
}

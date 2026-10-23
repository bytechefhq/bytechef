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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionOwnershipResolverTest {

    private final WorkspaceConnectionRepository workspaceConnectionRepository =
        mock(WorkspaceConnectionRepository.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final ConnectionOwnershipResolver resolver =
        new ConnectionOwnershipResolver(connectionService, workspaceConnectionRepository);

    private void stubConnection(PlatformType type) {
        Connection connection = new Connection();

        connection.setType(type);

        when(connectionService.fetchConnection(7L)).thenReturn(Optional.of(connection));
    }

    @Test
    void testResourceTypeMatchesTheTokenTheConnectionGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("Connection");
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceTheConnectionIsAssignedTo() {
        stubConnection(PlatformType.AUTOMATION);
        when(workspaceConnectionRepository.findByConnectionId(7L))
            .thenReturn(Optional.of(new WorkspaceConnection(7L, 42L)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofWorkspace(42L));
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheConnectionIsAssignedToNoWorkspace() {
        stubConnection(PlatformType.AUTOMATION);
        when(workspaceConnectionRepository.findByConnectionId(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAutomationConnectionResolverIgnoresEmbeddedRows() {
        stubConnection(PlatformType.EMBEDDED);
        lenient().when(workspaceConnectionRepository.findByConnectionId(7L))
            .thenReturn(Optional.of(new WorkspaceConnection(7L, 42L)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheConnectionDoesNotExist() {
        when(connectionService.fetchConnection(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }
}

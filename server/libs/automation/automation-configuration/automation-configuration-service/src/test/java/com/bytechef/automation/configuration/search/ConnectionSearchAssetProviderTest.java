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

package com.bytechef.automation.configuration.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionSearchAssetProviderTest {

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final WorkspaceConnectionService workspaceConnectionService = mock(WorkspaceConnectionService.class);
    private final ConnectionSearchAssetProvider connectionSearchAssetProvider = new ConnectionSearchAssetProvider(
        connectionService, workspaceConnectionService);

    @Test
    void testReturnsOnlyCallerWorkspaceConnectionsUpToLimit() {
        WorkspaceConnection ownConnection = mock(WorkspaceConnection.class);
        WorkspaceConnection ownConnection2 = mock(WorkspaceConnection.class);

        when(ownConnection.getConnectionId()).thenReturn(3L);
        when(ownConnection2.getConnectionId()).thenReturn(4L);
        when(workspaceConnectionService.getWorkspaceConnections(10L)).thenReturn(
            List.of(ownConnection, ownConnection2));

        List<Connection> connections = List.of(
            createConnection(1L, "Slack foreign"), createConnection(2L, "Slack foreign 2"),
            createConnection(3L, "Slack own"), createConnection(4L, "Slack own 2"));

        when(connectionService.getConnections(PlatformType.AUTOMATION)).thenReturn(connections);

        List<ConnectionSearchResult> results = connectionSearchAssetProvider.search("slack", 1, Set.of(10L));

        assertThat(results).extracting(ConnectionSearchResult::id)
            .containsExactly(3L);
    }

    @Test
    void testReturnsNothingWhenCallerWorkspacesHaveNoConnections() {
        when(workspaceConnectionService.getWorkspaceConnections(10L)).thenReturn(List.of());

        List<ConnectionSearchResult> results = connectionSearchAssetProvider.search("slack", 10, Set.of(10L));

        assertThat(results).isEmpty();

        verifyNoInteractions(connectionService);
    }

    private static Connection createConnection(long id, String name) {
        Connection connection = mock(Connection.class);

        when(connection.getId()).thenReturn(id);
        when(connection.getName()).thenReturn(name);

        return connection;
    }
}

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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectionSearchAssetProviderTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @InjectMocks
    private ConnectionSearchAssetProvider connectionSearchAssetProvider;

    @Test
    void testSearchStampsEachConnectionWithItsWorkspace() {
        when(connectionService.getConnections(PlatformType.AUTOMATION)).thenReturn(
            List.of(createConnection(1L, "slack prod"), createConnection(2L, "slack dev")));
        when(workspaceConnectionRepository.findByConnectionId(1L)).thenReturn(
            Optional.of(new WorkspaceConnection(1L, 7L)));
        when(workspaceConnectionRepository.findByConnectionId(2L)).thenReturn(Optional.empty());

        List<ConnectionSearchResult> connectionSearchResults = connectionSearchAssetProvider.search("SLACK", 10);

        assertThat(connectionSearchResults).containsExactly(
            new ConnectionSearchResult(1L, "slack prod", 7L), new ConnectionSearchResult(2L, "slack dev", null));
    }

    private static Connection createConnection(long id, String name) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName(name);

        return connection;
    }
}

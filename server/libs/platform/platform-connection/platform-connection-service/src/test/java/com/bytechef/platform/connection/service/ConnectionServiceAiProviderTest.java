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

package com.bytechef.platform.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceAiProviderTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private AiProviderConnectionRepository aiProviderConnectionRepository;

    @Mock
    private ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider;

    @Mock
    private CredentialStore databaseCredentialStore;

    private ConnectionServiceImpl connectionService() {
        // lenient: the delete test rejects before touching the provider, so this stub is unused there
        lenient().when(aiProviderConnectionRepositoryProvider.getIfAvailable())
            .thenReturn(aiProviderConnectionRepository);

        lenient().when(databaseCredentialStore.getType())
            .thenReturn(CredentialStoreType.DATABASE);
        lenient().when(databaseCredentialStore.getSecret(any()))
            .thenReturn(Map.of());

        return new ConnectionServiceImpl(
            List.of(databaseCredentialStore), connectionRepository, aiProviderConnectionRepositoryProvider);
    }

    @Test
    void testGetConnectionRoutesVirtualIdToProjector() {
        long id = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);
        Connection projected = new Connection();

        projected.setId(id);
        projected.setComponentName("openAi");

        when(aiProviderConnectionRepository.findById(id)).thenReturn(Optional.of(projected));

        Connection connection = connectionService().getConnection(id);

        assertThat(connection.getComponentName()).isEqualTo("openAi");
    }

    @Test
    void testDeleteRejectsVirtualId() {
        long id = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);

        assertThatThrownBy(() -> connectionService().delete(id))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("AI provider");
    }

    @Test
    void testGetConnectionsByTypeMergesProjected() {
        Connection projected = new Connection();

        projected.setId(AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0));
        projected.setComponentName("openAi");
        projected.setName("Open AI");

        when(connectionRepository.findAll(any(org.springframework.data.domain.Sort.class)))
            .thenReturn(List.of());
        when(aiProviderConnectionRepository.find(null, null, null)).thenReturn(List.of(projected));

        List<Connection> connections = connectionService().getConnections(PlatformType.AUTOMATION);

        assertThat(connections)
            .extracting(Connection::getComponentName)
            .contains("openAi");
    }

    @Test
    void testGetAiProviderConnectionsDelegatesToProjector() {
        Connection projected = new Connection();

        projected.setId(AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0));
        projected.setComponentName("openAi");
        projected.setName("Open AI");

        when(aiProviderConnectionRepository.find("openAi", null, 0)).thenReturn(List.of(projected));

        List<Connection> result = connectionService().getAiProviderConnections("openAi", null, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .getComponentName()).isEqualTo("openAi");
    }

    @Test
    void testGetAiProviderConnectionsEmptyWhenNoProjector() {
        ObjectProvider<AiProviderConnectionRepository> emptyProvider =
            org.mockito.Mockito.mock(ObjectProvider.class);

        lenient().when(emptyProvider.getIfAvailable())
            .thenReturn(null);
        lenient().when(databaseCredentialStore.getType())
            .thenReturn(CredentialStoreType.DATABASE);

        ConnectionServiceImpl service = new ConnectionServiceImpl(
            List.of(databaseCredentialStore), connectionRepository, emptyProvider);

        List<Connection> result = service.getAiProviderConnections(null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetConnectionsByIdsSplitsRealAndVirtual() {
        long realId = 1050L;
        long virtualId = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);

        Connection realConnection = new Connection();

        realConnection.setId(realId);
        realConnection.setComponentName("myComponent");
        realConnection.setName("Aardvark Connection");

        Connection projectedConnection = new Connection();

        projectedConnection.setId(virtualId);
        projectedConnection.setComponentName("openAi");
        projectedConnection.setName("Open AI");

        // Stub real-id lookup — lenient so it matches any list containing the real id
        lenient().when(connectionRepository.findAllByIdIn(any()))
            .thenReturn(List.of(realConnection));

        // Stub virtual-id lookup — lenient so it matches any list containing the virtual id
        lenient().when(aiProviderConnectionRepository.findAllByIdIn(any()))
            .thenReturn(List.of(projectedConnection));

        List<Connection> connections = connectionService().getConnections(List.of(realId, virtualId));

        assertThat(connections).hasSize(2);
        assertThat(connections)
            .extracting(Connection::getName)
            .containsExactly("Aardvark Connection", "Open AI");
    }
}

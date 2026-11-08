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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.platform.connection.config.ConnectionIntTestConfigurationSharedMocks;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
@SpringBootTest(classes = ConnectionIntTestConfiguration.class)
@Import({
    PostgreSQLContainerConfiguration.class, ConnectionServiceIntTest.AfterSaveListenerConfiguration.class
})
@ConnectionIntTestConfigurationSharedMocks
class ConnectionServiceIntTest {

    private static final long AI_PROVIDER_CONNECTION_ID = AiProviderConnectionId.encode(13, 0);

    @MockitoBean
    private AiProviderConnectionRepository aiProviderConnectionRepository;

    @Autowired
    private AfterSaveRecordingListener afterSaveRecordingListener;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void afterEach() {
        connectionRepository.deleteAll();
        tagRepository.deleteAll();

        afterSaveRecordingListener.clear();

        reset(aiProviderConnectionRepository);
    }

    @Test
    void testCreate() {
        Connection connection = getConnection();

        Tag tag = tagRepository.save(new Tag("tag1"));

        connection.setTags(List.of(tag));

        connection = connectionService.create(connection);

        assertThat(connection)
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(Validate.notNull(tag.getId(), "id")));
    }

    @Test
    void testCreateWithParameters() {
        AuthorizationType authorizationType = AuthorizationType.BASIC_AUTH;
        String componentName = "componentName";
        int connectionVersion = 1;
        Environment environment = Environment.PRODUCTION;
        String name = "name";
        Map<String, Object> parameters = Map.of("key1", "value1");
        PlatformType type = PlatformType.AUTOMATION;

        Connection connection = connectionService.create(
            authorizationType, componentName, connectionVersion, environment.ordinal(), name, parameters, type);

        assertThat(connection)
            .hasFieldOrPropertyWithValue("authorizationType", authorizationType)
            .hasFieldOrPropertyWithValue("componentName", componentName)
            .hasFieldOrPropertyWithValue("connectionVersion", connectionVersion)
            .hasFieldOrPropertyWithValue("environment", environment.ordinal())
            .hasFieldOrPropertyWithValue("name", name)
            .hasFieldOrPropertyWithValue("parameters", parameters)
            .hasFieldOrPropertyWithValue("type", type);
    }

    @Test
    void testCreateWithAuthorizationTypNone() {
        String componentName = "componentName";
        int connectionVersion = 1;
        Environment environment = Environment.PRODUCTION;
        String name = "name";
        Map<String, Object> parameters = Map.of("key1", "value1");
        PlatformType type = PlatformType.AUTOMATION;

        Connection connection = connectionService.create(
            null, componentName, connectionVersion, environment.ordinal(), name, parameters, type);

        assertThat(connection)
            .hasFieldOrPropertyWithValue("authorizationType", connection.getAuthorizationType())
            .hasFieldOrPropertyWithValue("componentName", componentName)
            .hasFieldOrPropertyWithValue("connectionVersion", connectionVersion)
            .hasFieldOrPropertyWithValue("environment", environment.ordinal())
            .hasFieldOrPropertyWithValue("name", name)
            .hasFieldOrPropertyWithValue("parameters", parameters)
            .hasFieldOrPropertyWithValue("type", type);
    }

    @Test
    void testDelete() {
        Connection connection = connectionRepository.save(getConnection());

        connectionService.delete(Validate.notNull(connection.getId(), "id"));

        assertThat(connectionRepository.findById(connection.getId())).isNotPresent();
    }

    @Test
    void testGetConnection() {
        Connection connection = getConnection();

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        connection.setTags(List.of(tag));

        connection = connectionRepository.save(connection);

        assertThat(connectionService.getConnection(Validate.notNull(connection.getId(), "id"))).isEqualTo(connection);
        assertThat(connectionService.getConnections(null, null, tag.getId(), null, PlatformType.AUTOMATION)).hasSize(1);
    }

    @Test
    void testUpdateConnectionCredentialStatusMarksCredentialsStatusUpdatedForAfterSaveListeners() {
        Connection connection = connectionRepository.save(getConnection());

        afterSaveRecordingListener.clear();

        connectionService.updateConnectionCredentialStatus(
            Validate.notNull(connection.getId(), "id"), Connection.CredentialStatus.INVALID);

        assertThat(afterSaveRecordingListener.getCredentialsStatusUpdatedFlags()).containsExactly(true);
    }

    @Test
    void getGetConnections() {
        connectionRepository.save(getConnection());

        assertThat(connectionService.getConnections(null, null, null, null, PlatformType.AUTOMATION)).hasSize(1);
    }

    @Test
    void testGetConnectionReadsAnAiProviderConnectionFromItsProjection() {
        Connection aiProviderConnection = getAiProviderConnection("Open AI");

        when(aiProviderConnectionRepository.findById(AI_PROVIDER_CONNECTION_ID))
            .thenReturn(Optional.of(aiProviderConnection));

        assertThat(connectionService.getConnection(AI_PROVIDER_CONNECTION_ID)).isSameAs(aiProviderConnection);
    }

    @Test
    void testGetConnectionsMergesAiProviderConnectionsByName() {
        Connection connection = getConnection();

        connection.setName("Zebra");

        connectionService.create(connection);

        when(aiProviderConnectionRepository.find(null, null, null))
            .thenReturn(List.of(getAiProviderConnection("Open AI")));

        assertThat(connectionService.getConnections(PlatformType.AUTOMATION))
            .extracting(Connection::getName)
            .containsExactly("Open AI", "Zebra");
        assertThat(connectionService.getConnections(PlatformType.EMBEDDED)).isEmpty();
    }

    @Test
    void testGetConnectionsByIdsSplitsRealAndAiProviderIds() {
        Connection connection = connectionService.create(getConnection());

        long connectionId = Validate.notNull(connection.getId(), "id");

        when(aiProviderConnectionRepository.findAllByIdIn(List.of(AI_PROVIDER_CONNECTION_ID)))
            .thenReturn(List.of(getAiProviderConnection("Open AI")));

        assertThat(connectionService.getConnections(List.of(connectionId, AI_PROVIDER_CONNECTION_ID)))
            .extracting(Connection::getName)
            .containsExactly("name", "Open AI");
    }

    @Test
    void testGetAiProviderConnectionsDelegatesToTheProjection() {
        when(aiProviderConnectionRepository.find("openAi", null, 0))
            .thenReturn(List.of(getAiProviderConnection("Open AI")));

        assertThat(connectionService.getAiProviderConnections("openAi", null, 0))
            .extracting(Connection::getComponentName)
            .containsExactly("openAi");
    }

    @Test
    void testAiProviderConnectionsAreReadOnly() {
        List<ThrowingCallable> writes = List.of(
            () -> connectionService.delete(AI_PROVIDER_CONNECTION_ID),
            () -> connectionService.update(AI_PROVIDER_CONNECTION_ID, List.of()),
            () -> connectionService.update(AI_PROVIDER_CONNECTION_ID, "name", List.of(), 0),
            () -> connectionService.updateConnectionCredentialStatus(
                AI_PROVIDER_CONNECTION_ID, Connection.CredentialStatus.VALID),
            () -> connectionService.updateConnectionStatus(AI_PROVIDER_CONNECTION_ID, ConnectionStatus.ACTIVE),
            () -> connectionService.updateCreatedBy(AI_PROVIDER_CONNECTION_ID, "user"),
            () -> connectionService.updateConnectionParameters(AI_PROVIDER_CONNECTION_ID, Map.of()),
            () -> connectionService.replaceConnectionParameters(AI_PROVIDER_CONNECTION_ID, Map.of()));

        for (ThrowingCallable write : writes) {
            assertThatThrownBy(write)
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("AI provider");
        }
    }

    private static Connection getAiProviderConnection(String name) {
        Connection connection = new Connection();

        connection.setComponentName("openAi");
        connection.setId(AI_PROVIDER_CONNECTION_ID);
        connection.setManaged(true);
        connection.setName(name);

        return connection;
    }

    private static Connection getConnection() {
        return Connection.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"))
            .type(PlatformType.AUTOMATION)
            .build();
    }

    @TestConfiguration
    static class AfterSaveListenerConfiguration {

        @Bean
        AfterSaveRecordingListener afterSaveRecordingListener() {
            return new AfterSaveRecordingListener();
        }
    }

    static class AfterSaveRecordingListener extends AbstractRelationalEventListener<Connection> {

        private final List<Boolean> credentialsStatusUpdatedFlags = Collections.synchronizedList(new ArrayList<>());

        @Override
        protected void onAfterSave(AfterSaveEvent<Connection> afterSaveEvent) {
            Connection connection = afterSaveEvent.getEntity();

            credentialsStatusUpdatedFlags.add(connection.isCredentialsStatusUpdated());
        }

        void clear() {
            credentialsStatusUpdatedFlags.clear();
        }

        List<Boolean> getCredentialsStatusUpdatedFlags() {
            return List.copyOf(credentialsStatusUpdatedFlags);
        }
    }
}

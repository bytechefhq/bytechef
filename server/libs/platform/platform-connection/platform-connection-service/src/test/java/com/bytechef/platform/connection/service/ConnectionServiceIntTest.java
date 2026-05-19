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

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.platform.connection.config.ConnectionIntTestConfigurationSharedMocks;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ConnectionIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ConnectionIntTestConfigurationSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
public class ConnectionServiceIntTest {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestExternalConnectionCredentialStore testExternalConnectionCredentialStore;

    @AfterEach
    public void afterEach() {
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
        testExternalConnectionCredentialStore.setReadOnly(false);
    }

    @Test
    public void testCreate() {
        Connection connection = getConnection();

        Tag tag = tagRepository.save(new Tag("tag1"));

        connection.setTags(List.of(tag));

        connection = connectionService.create(connection);

        assertThat(connection)
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(Validate.notNull(tag.getId(), "id")));
    }

    @Test
    public void testCreateWithParameters() {
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
    public void testCreateWithAuthorizationTypNone() {
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
    public void testDelete() {
        Connection connection = connectionRepository.save(getConnection());

        connectionService.delete(Validate.notNull(connection.getId(), "id"));

        assertThat(connectionRepository.findById(connection.getId())).isNotPresent();
    }

    @Test
    public void testGetConnection() {
        Connection connection = getConnection();

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        connection.setTags(List.of(tag));

        connection = connectionRepository.save(connection);

        assertThat(connectionService.getConnection(Validate.notNull(connection.getId(), "id"))).isEqualTo(connection);
        assertThat(connectionService.getConnections(null, null, tag.getId(), null, PlatformType.AUTOMATION)).hasSize(1);
    }

    @Test
    public void getGetConnections() {
        connectionRepository.save(getConnection());

        assertThat(connectionService.getConnections(null, null, null, null, PlatformType.AUTOMATION)).hasSize(1);
    }

    @Test
    public void testCreateExternalBackedConnection() {
        Connection connection = getConnection();

        connection.setCredentialStoreType(CredentialStoreType.HASHICORP_VAULT);

        Connection saved = connectionService.create(connection);

        Long id = Validate.notNull(saved.getId(), "id");

        assertThat(saved.getCredentialStoreType())
            .isEqualTo(CredentialStoreType.HASHICORP_VAULT);
        assertThat(saved.getCredentialRef()).isNotBlank();
        assertThat((Map<String, Object>) saved.getParameters()).containsEntry("key1", "value1");

        // Reload from DB and verify the row carries the ref and the service re-populates parameters on read.
        Connection persistedRow = Validate.notNull(
            connectionRepository.findById(id)
                .orElse(null),
            "persisted row");

        assertThat(persistedRow.getParameters()).isEmpty();
        assertThat(persistedRow.getCredentialRef()).isNotBlank();

        Connection viaService = connectionService.getConnection(id);

        assertThat((Map<String, Object>) viaService.getParameters()).containsEntry("key1", "value1");
    }

    @Test
    public void testUpdateParametersOnExternalStore() {
        Connection connection = getConnection();

        connection.setCredentialStoreType(CredentialStoreType.HASHICORP_VAULT);

        Connection saved = connectionService.create(connection);
        Long id = Validate.notNull(saved.getId(), "id");

        Connection updated = connectionService.updateConnectionParameters(
            id, Map.of("key2", "value2"));

        assertThat((Map<String, Object>) updated.getParameters())
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
    }

    @Test
    public void testDeleteExternalBackedConnection() {
        Connection connection = getConnection();

        connection.setCredentialStoreType(CredentialStoreType.HASHICORP_VAULT);

        Connection saved = connectionService.create(connection);
        Long id = Validate.notNull(saved.getId(), "id");

        connectionService.delete(id);

        assertThat(connectionRepository.findById(id)).isNotPresent();
        assertThat(testExternalConnectionCredentialStore.getSecret(saved)).isEmpty();
    }

    @Test
    public void testCreateRefusedWhenStoreReadOnly() {
        testExternalConnectionCredentialStore.setReadOnly(true);

        Connection connection = getConnection();

        connection.setCredentialStoreType(CredentialStoreType.HASHICORP_VAULT);

        assertThatThrownBy(() -> connectionService.create(connection))
            .isInstanceOf(ReadOnlyCredentialStoreException.class);
    }

    @Test
    public void testRegisterExistingForReadOnlyStore() {
        // Pre-seed a "secret" in the external store keyed by a known ref while the store is writable.
        Connection seed = new Connection();
        seed.setCredentialRef("preprovisioned-ref-123");

        testExternalConnectionCredentialStore.storeSecret(seed, Map.of("apiKey", "secret-from-vault"));

        // Flip to read-only and register the existing secret as a ByteChef connection.
        testExternalConnectionCredentialStore.setReadOnly(true);

        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("registered");
        connection.setType(PlatformType.AUTOMATION);

        Connection registered = connectionService.registerExisting(
            connection, CredentialStoreType.HASHICORP_VAULT, "preprovisioned-ref-123");

        Connection viaService = connectionService.getConnection(Validate.notNull(registered.getId(), "id"));

        assertThat((Map<String, Object>) viaService.getParameters()).containsEntry("apiKey", "secret-from-vault");
        assertThat(viaService.getCredentialRef()).isEqualTo("preprovisioned-ref-123");
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
}

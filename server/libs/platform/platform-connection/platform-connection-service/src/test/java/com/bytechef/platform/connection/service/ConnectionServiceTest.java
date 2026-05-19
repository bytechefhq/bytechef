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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    private static final long CONNECTION_ID = 10L;
    private static final String ADMIN_USER = "admin@example.com";

    @Mock
    private ConnectionRepository connectionRepository;

    private ConnectionServiceImpl connectionService;

    @BeforeEach
    void setUp() {
        connectionService = new ConnectionServiceImpl(List.of(), connectionRepository);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                ADMIN_USER, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateConnectionStatus() {
        Connection connection = new Connection();

        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(connection);

        Connection result =
            connectionService.updateConnectionStatus(CONNECTION_ID, ConnectionStatus.PENDING_REASSIGNMENT);

        assertThat(result).isNotNull();

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionRepository).save(captor.capture());

        assertThat(captor.getValue()
            .getStatus()).isEqualTo(ConnectionStatus.PENDING_REASSIGNMENT);
    }

    @Test
    void testUpdateConnectionStatusNotFound() {
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.updateConnectionStatus(CONNECTION_ID, ConnectionStatus.ACTIVE))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining(String.valueOf(CONNECTION_ID));
    }

    @Test
    void testUpdateCreatedBy() {
        Connection connection = new Connection();

        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(connection);

        String newOwner = "new-owner@example.com";

        Connection result = connectionService.updateCreatedBy(CONNECTION_ID, newOwner);

        assertThat(result).isNotNull();

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionRepository).save(captor.capture());

        assertThat(captor.getValue()
            .getCreatedBy()).isEqualTo(newOwner);
    }

    @Test
    void testUpdateCreatedByNotFound() {
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.updateCreatedBy(CONNECTION_ID, "new-owner@example.com"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining(String.valueOf(CONNECTION_ID));
    }

    @Test
    void testUpdateVisibility() {
        Connection connection = new Connection();

        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(connection);

        Connection result = connectionService.updateVisibility(CONNECTION_ID, ResourceVisibility.WORKSPACE);

        assertThat(result).isNotNull();

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionRepository).save(captor.capture());

        assertThat(captor.getValue()
            .getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testUpdateVisibilityNotFound() {
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.updateVisibility(CONNECTION_ID, ResourceVisibility.ORGANIZATION))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining(String.valueOf(CONNECTION_ID));
    }

    @Test
    void testValidateConnectionsActiveAllActive() {
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setStatus(ConnectionStatus.ACTIVE);

        when(connectionRepository.findAllByIdIn(List.of(CONNECTION_ID))).thenReturn(List.of(connection));

        connectionService.validateConnectionsActive(List.of(CONNECTION_ID));
    }

    @Test
    void testValidateConnectionsActiveRejectsPendingReassignment() {
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setStatus(ConnectionStatus.PENDING_REASSIGNMENT);

        when(connectionRepository.findAllByIdIn(List.of(CONNECTION_ID))).thenReturn(List.of(connection));

        assertThatThrownBy(() -> connectionService.validateConnectionsActive(List.of(CONNECTION_ID)))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("PENDING_REASSIGNMENT")
            .extracting("errorKey")
            .isEqualTo(ConnectionErrorType.CONNECTION_NOT_ACTIVE.getErrorKey());
    }

    @Test
    void testValidateConnectionsActiveShortCircuitsOnEmpty() {
        connectionService.validateConnectionsActive(List.of());
        connectionService.validateConnectionsActive(null);
    }

    @Test
    void testGetInactiveConnectionsReturnsRevokedConnection() {
        // Regression coverage for the fail-closed predicate change. REVOKED is also a terminal
        // status; workflow execution must block on any non-ACTIVE state, not just
        // PENDING_REASSIGNMENT (which the existing testValidateConnectionsActiveRejectsPendingReassignment
        // already exercises).
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setStatus(ConnectionStatus.REVOKED);

        when(connectionRepository.findAllByIdIn(List.of(CONNECTION_ID))).thenReturn(List.of(connection));

        List<Connection> result = connectionService.getInactiveConnections(List.of(CONNECTION_ID));

        assertThat(result).containsExactly(connection);
    }

    @Test
    void testGetInactiveConnectionsEmptyShortCircuits() {
        assertThat(connectionService.getInactiveConnections(null)).isEmpty();
        assertThat(connectionService.getInactiveConnections(List.of())).isEmpty();
    }

    @Test
    void testMetadataUpdateTagsDoesNotWriteExternalSecretToInlineParametersColumn() {
        // Regression: update(id, tagIds) called getConnection(id) which populated parameters from
        // the external store, then saved the connection — writing the secret into the inline column.
        CredentialStore awsStore = Mockito.mock(CredentialStore.class);

        lenient().when(awsStore.getType())
            .thenReturn(CredentialStoreType.AWS_SECRETS_MANAGER);
        lenient().when(awsStore.isReadOnly())
            .thenReturn(false);
        doReturn(Map.of("apiKey", "secret-value")).when(awsStore)
            .getSecret(any());

        CredentialStore dbStore = Mockito.mock(CredentialStore.class);

        lenient().when(dbStore.getType())
            .thenReturn(CredentialStoreType.DATABASE);
        lenient().when(dbStore.isReadOnly())
            .thenReturn(false);
        lenient().doReturn(Map.of())
            .when(dbStore)
            .getSecret(any());

        ConnectionServiceImpl serviceWithStores = new ConnectionServiceImpl(
            List.of(dbStore, awsStore), connectionRepository);

        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setCredentialStoreType(CredentialStoreType.AWS_SECRETS_MANAGER);

        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));

        // Snapshot the parameters at the exact moment save() is called, before populateParameters
        // mutates the returned object — this represents what Spring Data JDBC would persist to DB.
        AtomicReference<Map<String, ?>> parametersAtSaveTime = new AtomicReference<>();

        when(connectionRepository.save(any(Connection.class))).thenAnswer(inv -> {
            Connection savedConnection = inv.getArgument(0);

            parametersAtSaveTime.set(new HashMap<>(savedConnection.getParameters()));

            return savedConnection;
        });

        serviceWithStores.update(CONNECTION_ID, List.of(42L));

        // The inline parameters column must be empty for an external-store row.
        assertThat(parametersAtSaveTime.get()).isEmpty();
        assertThat(connection.getCredentialStoreType()).isEqualTo(CredentialStoreType.AWS_SECRETS_MANAGER);
    }
}

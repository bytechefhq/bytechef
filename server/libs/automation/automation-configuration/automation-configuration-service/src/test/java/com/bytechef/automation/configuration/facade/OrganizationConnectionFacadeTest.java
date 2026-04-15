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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class OrganizationConnectionFacadeTest {

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private ConnectionService connectionService;

    private OrganizationConnectionFacadeImpl organizationConnectionFacade;

    @BeforeEach
    void setUp() {
        organizationConnectionFacade = new OrganizationConnectionFacadeImpl(
            connectionFacade, connectionService, "EE");
    }

    @Test
    void testCreate() {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);
        long expectedConnectionId = 42L;

        when(connectionFacade.create(connectionDTO, PlatformType.AUTOMATION)).thenReturn(expectedConnectionId);

        long result = organizationConnectionFacade.create(connectionDTO);

        assertThat(result).isEqualTo(expectedConnectionId);
        verify(connectionFacade).create(connectionDTO, PlatformType.AUTOMATION);
    }

    @Test
    void testCreateOnCeEditionIsRejected() {
        // ORGANIZATION visibility is EE-only. A CE admin hitting the GraphQL endpoint must not be
        // able to bypass ConnectionFacadeImpl.create's CE-forces-PRIVATE guard by chaining
        // updateVisibility(ORGANIZATION) afterwards. Fails fast before touching the connection.
        OrganizationConnectionFacadeImpl ceFacade = new OrganizationConnectionFacadeImpl(
            connectionFacade, connectionService, "CE");

        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);

        assertThatThrownBy(() -> ceFacade.create(connectionDTO))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Enterprise Edition");

        verify(connectionFacade, never()).create(any(), any());
        verify(connectionService, never()).updateVisibility(anyLong(), any());
    }

    @Test
    void testGetOrganizationConnectionsOnCeEditionReturnsEmpty() {
        // Defense in depth: even a read should not leak organization connections on CE.
        OrganizationConnectionFacadeImpl ceFacade = new OrganizationConnectionFacadeImpl(
            connectionFacade, connectionService, "CE");

        assertThat(ceFacade.getOrganizationConnections(null)).isEmpty();

        verify(connectionService, never()).getConnectionsByVisibility(any(), any());
    }

    @Test
    void testDeleteOrganizationConnection() {
        long connectionId = 10L;
        Connection connection = mock(Connection.class);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
        when(connection.getVisibility()).thenReturn(ConnectionVisibility.ORGANIZATION);

        organizationConnectionFacade.delete(connectionId);

        verify(connectionFacade).delete(connectionId);
    }

    @Test
    void testDeleteNonOrganizationConnection() {
        long connectionId = 10L;
        Connection connection = mock(Connection.class);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);

        assertThatThrownBy(() -> organizationConnectionFacade.delete(connectionId))
            .isInstanceOf(ConfigurationException.class);

        verify(connectionFacade, never()).delete(any());
    }

    @Test
    void testGetOrganizationConnections() {
        Connection orgConnection = mock(Connection.class);

        when(connectionService.getConnectionsByVisibility(ConnectionVisibility.ORGANIZATION, PlatformType.AUTOMATION))
            .thenReturn(List.of(orgConnection));

        ConnectionDTO expectedDTO = mock(ConnectionDTO.class);

        when(connectionFacade.toConnectionDTOs(List.of(orgConnection))).thenReturn(List.of(expectedDTO));

        List<ConnectionDTO> result = organizationConnectionFacade.getOrganizationConnections(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(expectedDTO);
    }

    @Test
    void testUpdateOrganizationConnection() {
        long connectionId = 10L;
        Connection connection = mock(Connection.class);
        ConnectionDTO expectedDTO = mock(ConnectionDTO.class);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
        when(connection.getVisibility()).thenReturn(ConnectionVisibility.ORGANIZATION);
        when(connectionFacade.getConnection(connectionId)).thenReturn(expectedDTO);

        ConnectionDTO result = organizationConnectionFacade.update(connectionId, "newName", List.of(1L, 2L), 1);

        assertThat(result).isEqualTo(expectedDTO);
        verify(connectionFacade).update(eq(connectionId), eq("newName"), any(), eq(1));
    }

    @Test
    void testUpdateNonOrganizationConnection() {
        long connectionId = 10L;
        Connection connection = mock(Connection.class);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
        when(connection.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);

        assertThatThrownBy(
            () -> organizationConnectionFacade.update(connectionId, "newName", List.of(1L), 1))
                .isInstanceOf(ConfigurationException.class);

        verify(connectionFacade, never()).update(anyLong(), anyString(), any(), anyInt());
    }

    @Test
    void testCreateSetsOrganizationVisibility() {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);
        long expectedConnectionId = 99L;

        when(connectionFacade.create(connectionDTO, PlatformType.AUTOMATION)).thenReturn(expectedConnectionId);

        organizationConnectionFacade.create(connectionDTO);

        verify(connectionService).updateVisibility(expectedConnectionId, ConnectionVisibility.ORGANIZATION);
    }
}

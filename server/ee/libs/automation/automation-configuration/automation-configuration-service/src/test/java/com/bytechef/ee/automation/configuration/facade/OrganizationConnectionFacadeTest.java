/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

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
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
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
        organizationConnectionFacade = new OrganizationConnectionFacadeImpl(connectionFacade, connectionService);
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
    void testDeleteOrganizationConnection() {
        long connectionId = 10L;
        Connection connection = mock(Connection.class);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
        when(connection.getVisibility()).thenReturn(ResourceVisibility.ORGANIZATION);

        organizationConnectionFacade.delete(connectionId);

        verify(connectionFacade).delete(connectionId);
    }

    @Test
    void testDeleteNonOrganizationConnection() {
        long connectionId = 10L;
        Connection connection = mock(Connection.class);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
        when(connection.getVisibility()).thenReturn(ResourceVisibility.PRIVATE);

        assertThatThrownBy(() -> organizationConnectionFacade.delete(connectionId))
            .isInstanceOf(ConfigurationException.class);

        verify(connectionFacade, never()).delete(any());
    }

    @Test
    void testGetOrganizationConnections() {
        Connection orgConnection = mock(Connection.class);

        when(connectionService.getConnectionsByVisibility(ResourceVisibility.ORGANIZATION, PlatformType.AUTOMATION))
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
        when(connection.getVisibility()).thenReturn(ResourceVisibility.ORGANIZATION);
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
        when(connection.getVisibility()).thenReturn(ResourceVisibility.WORKSPACE);

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

        verify(connectionService).updateVisibility(expectedConnectionId, ResourceVisibility.ORGANIZATION);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserConnectionFacadeTest {

    @Mock
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    private ConnectedUserConnectionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ConnectedUserConnectionFacadeImpl(
            connectedUserConnectionService, connectedUserService, connectionFacade, integrationInstanceService);
    }

    @Test
    void testCreateConnectedUserConnection() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(5L);

        long connectionId = facade.createConnectedUserConnection(1L, connectionDTO);

        assertThat(connectionId).isEqualTo(5L);

        verify(connectedUserConnectionService).create(1L, 5L);
    }

    @Test
    void testGetConnectionsMergesInstanceAndConnectedUserConnectionIds() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(10L);

        when(integrationInstanceService.getIntegrationInstances(1L, "slack", Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(1L)).thenReturn(List.of(20L));
        when(connectionFacade.getConnections(List.of(10L, 20L), PlatformType.EMBEDDED)).thenReturn(List.of());

        facade.getConnections(1L, "slack", List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactly(10L, 20L);
    }

    @Test
    void testGetConnectionsWithNullComponentNameReturnsAllOwnedConnections() {
        stubConnectedUserOwnership(List.of(1L, 2L), List.of(3L));

        ConnectionDTO slackConnection = ConnectionDTO.builder()
            .id(1L)
            .componentName("slack")
            .build();
        ConnectionDTO hubspotConnection = ConnectionDTO.builder()
            .id(2L)
            .componentName("hubspot")
            .build();
        ConnectionDTO githubConnection = ConnectionDTO.builder()
            .id(3L)
            .componentName("github")
            .build();

        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenReturn(List.of(slackConnection, hubspotConnection, githubConnection));

        List<ConnectionDTO> connectionDTOs = facade.getConnections(7L, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(connectionDTOs).containsExactlyInAnyOrder(slackConnection, hubspotConnection, githubConnection);
    }

    @Test
    void testDeleteConnectedUserConnectionRejectsForeignId() {
        stubConnectedUserOwnership(List.of(1L), List.of());
        stubOwnedConnectionDTOs(List.of(1L));

        assertThrows(NoSuchElementException.class, () -> facade.deleteConnectedUserConnection(7L, 9L));

        verify(connectionFacade, never()).delete(any());
    }

    @Test
    void testDeleteConnectedUserConnectionDelegates() {
        stubConnectedUserOwnership(List.of(1L), List.of());
        stubOwnedConnectionDTOs(List.of(1L));

        facade.deleteConnectedUserConnection(7L, 1L);

        verify(connectionFacade).delete(1L);
    }

    @Test
    void testReauthorizeDelegates() {
        stubConnectedUserOwnership(List.of(1L), List.of());
        stubOwnedConnectionDTOs(List.of(1L));

        Map<String, Object> parameters = Map.of("apiKey", "new");

        facade.reauthorizeConnectedUserConnection(7L, 1L, parameters);

        verify(connectionFacade).updateAuthorization(1L, parameters);
    }

    @Test
    void testReauthorizeRejectsForeignId() {
        stubConnectedUserOwnership(List.of(1L), List.of());
        stubOwnedConnectionDTOs(List.of(1L));

        Map<String, Object> parameters = Map.of("apiKey", "new");

        assertThrows(
            NoSuchElementException.class, () -> facade.reauthorizeConnectedUserConnection(7L, 9L, parameters));

        verify(connectionFacade, never()).updateAuthorization(anyLong(), any());
    }

    /**
     * Stubs the connected-user lookup and connection-id sources that {@code requireOwned} walks (via
     * {@code getConnections(connectedUserId, null, List.of())}) for connected user id 7:
     * {@code integrationInstanceConnectionIds} sourced from integration instances and
     * {@code connectedUserConnectionIds} sourced from the connected-user-linked connections.
     */
    private void stubConnectedUserOwnership(
        List<Long> connectedUserConnectionIds, List<Long> integrationInstanceConnectionIds) {

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(7L);
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        when(connectedUserService.getConnectedUser(7L)).thenReturn(connectedUser);

        List<IntegrationInstance> integrationInstances = integrationInstanceConnectionIds.stream()
            .map(connectionId -> {
                IntegrationInstance integrationInstance = new IntegrationInstance();

                integrationInstance.setConnectionId(connectionId);

                return integrationInstance;
            })
            .toList();

        when(integrationInstanceService.getConnectedUserIntegrationInstances(7L, Environment.PRODUCTION))
            .thenReturn(integrationInstances);
        when(connectedUserConnectionService.getConnectionIds(7L)).thenReturn(connectedUserConnectionIds);
    }

    /**
     * Stubs {@code connectionFacade.getConnections} with one {@link ConnectionDTO} per owned id, so
     * {@code requireOwned}'s membership check resolves.
     */
    private void stubOwnedConnectionDTOs(List<Long> ownedConnectionIds) {
        List<ConnectionDTO> ownedConnectionDTOs = ownedConnectionIds.stream()
            .map(connectionId -> ConnectionDTO.builder()
                .id(connectionId)
                .build())
            .toList();

        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED))).thenReturn(ownedConnectionDTOs);
    }
}

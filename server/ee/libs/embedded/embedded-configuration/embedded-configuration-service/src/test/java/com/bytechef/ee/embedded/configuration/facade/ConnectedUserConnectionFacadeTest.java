/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
            new ConnectedUserConnectionMembership(connectedUserConnectionService, integrationInstanceService),
            connectedUserConnectionService, connectedUserService, connectionFacade);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateConnectedUserConnection() {
        authenticateAsConnectedUser("external-user-1", 0L);

        mockConnectedUser(1L, "external-user-1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(5L);

        long connectionId = facade.createConnectedUserConnection(1L, null, connectionDTO);

        assertThat(connectionId).isEqualTo(5L);

        verify(connectedUserConnectionService).create(1L, 5L);
    }

    @Test
    void testCreateConnectedUserConnectionForAnotherConnectedUserIsDenied() {
        authenticateAsConnectedUser("external-user-2", 0L);

        mockConnectedUser(1L, "external-user-1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        assertThatThrownBy(() -> facade.createConnectedUserConnection(1L, null, connectionDTO))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }

    @Test
    void testCreateConnectedUserConnectionWithMatchingLoginButNoConnectedUserPrincipalIsDenied() {
        authenticate("external-user-1");

        mockConnectedUser(1L, "external-user-1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        assertThatThrownBy(() -> facade.createConnectedUserConnection(1L, null, connectionDTO))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }

    @Test
    void testCreateConnectedUserConnectionFromAnotherEnvironmentIsDenied() {
        authenticateAsConnectedUser("external-user-1", 2L);

        mockConnectedUser(1L, "external-user-1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        assertThatThrownBy(() -> facade.createConnectedUserConnection(1L, null, connectionDTO))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }

    @Test
    void testCreateConnectedUserConnectionTakesTheEnvironmentFromThePrincipal() {
        authenticateAsConnectedUser("external-user-1", 2L);

        ConnectedUser connectedUser = mockConnectedUser(1L, "external-user-1");

        when(connectedUser.getEnvironmentId()).thenReturn(2L);
        when(connectionFacade.create(any(), eq(PlatformType.EMBEDDED))).thenReturn(5L);

        facade.createConnectedUserConnection(
            1L, null, ConnectionDTO.builder()
                .environmentId(0)
                .build());
        facade.createConnectedUserConnection(
            1L, 2L, ConnectionDTO.builder()
                .environmentId(2)
                .build());

        ArgumentCaptor<ConnectionDTO> connectionDTOArgumentCaptor = ArgumentCaptor.captor();

        verify(connectionFacade, times(2)).create(connectionDTOArgumentCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(connectionDTOArgumentCaptor.getAllValues())
            .extracting(ConnectionDTO::environmentId)
            .containsExactly(2, 2);
    }

    @Test
    void testCreateConnectedUserConnectionInAnotherEnvironmentThanThePrincipalsIsDenied() {
        authenticateAsConnectedUser("external-user-1", 2L);

        ConnectedUser connectedUser = mockConnectedUser(1L, "external-user-1");

        when(connectedUser.getEnvironmentId()).thenReturn(2L);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .environmentId(0)
            .build();

        assertThatThrownBy(() -> facade.createConnectedUserConnection(1L, 0L, connectionDTO))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }

    @Test
    void testCreateConnectedUserConnectionAsTenantAdmin() {
        authenticate("admin@localhost.com", AuthorityConstants.ADMIN);

        mockConnectedUser(1L, "external-user-1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(5L);

        assertThat(facade.createConnectedUserConnection(1L, null, connectionDTO)).isEqualTo(5L);

        verify(connectedUserConnectionService).create(1L, 5L);
    }

    @Test
    void testGetConnectedUserConnectionsOfAnotherConnectedUserIsDenied() {
        authenticateAsConnectedUser("external-user-2", 0L);

        mockConnectedUser(1L, "external-user-1");

        assertThatThrownBy(() -> facade.getConnectedUserConnections(1L, "slack"))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnections(any(), any());
    }

    @Test
    void testGetConnectedUserConnectionsWithoutAuthenticationIsDenied() {
        mockConnectedUser(1L, "external-user-1");

        assertThatThrownBy(() -> facade.getConnectedUserConnections(1L, "slack"))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnections(any(), any());
    }

    @Test
    void testGetConnectedUserConnectionsOfTheCurrentConnectedUser() {
        authenticateAsConnectedUser("external-user-1", 0L);

        ConnectedUser connectedUser = mockConnectedUser(1L, "external-user-1");

        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(connectedUserConnectionService.getConnectionIds(1L)).thenReturn(List.of(20L));
        when(connectionFacade.getConnections(List.of(20L), PlatformType.EMBEDDED)).thenReturn(List.of());

        assertThat(facade.getConnectedUserConnections(1L, "slack")).isEmpty();

        verify(connectionFacade).getConnections(List.of(20L), PlatformType.EMBEDDED);
    }

    @Test
    void testGetConnectionsMergesInstanceAndConnectedUserConnectionIds() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(10L);

        when(integrationInstanceService.getConnectedUserIntegrationInstances(1L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(1L)).thenReturn(List.of(20L));
        when(connectionFacade.getConnections(List.of(10L, 20L), PlatformType.EMBEDDED)).thenReturn(List.of());

        facade.getConnections(1L, "slack");

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactly(10L, 20L);
    }

    @Test
    void testGetConnectionsFiltersByComponentName() {
        ConnectedUser connectedUser = mockConnectedUser(1L, "external-user-1");

        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(connectedUserConnectionService.getConnectionIds(1L)).thenReturn(List.of(20L, 40L));

        ConnectionDTO slackConnectionDTO = getConnectionDTO(20L, "slack");
        ConnectionDTO githubConnectionDTO = getConnectionDTO(40L, "github");

        when(connectionFacade.getConnections(List.of(20L, 40L), PlatformType.EMBEDDED))
            .thenReturn(List.of(slackConnectionDTO, githubConnectionDTO));

        assertThat(facade.getConnections(1L, "slack")).containsExactly(slackConnectionDTO);
        assertThat(facade.getConnections(1L, null)).containsExactly(slackConnectionDTO, githubConnectionDTO);
    }

    @Test
    void testGetConnectionsListsNoConnectionOfAnotherConnectedUser() {
        ConnectedUser connectedUser = mockConnectedUser(1L, "external-user-1");

        when(connectedUser.getEnvironment()).thenReturn(Environment.DEVELOPMENT);
        when(connectedUserConnectionService.getConnectionIds(1L)).thenReturn(List.of(20L));

        Map<Long, ConnectionDTO> connectionDTOs = Map.of(
            20L, getConnectionDTO(20L, "slack"), 40L, getConnectionDTO(40L, "slack"));

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(connectionDTOs::get)
                    .filter(Objects::nonNull)
                    .toList();
            });

        assertThat(facade.getConnections(1L, "slack"))
            .extracting(ConnectionDTO::id)
            .containsExactly(20L);

        verify(connectionFacade, never()).getConnections(any(), any(), any(), any(), any(), any());
    }

    private static ConnectionDTO getConnectionDTO(long id, String componentName) {
        return ConnectionDTO.builder()
            .componentName(componentName)
            .id(id)
            .build();
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                login, null, Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .toList()));

        SecurityContextHolder.setContext(securityContext);
    }

    private static void authenticateAsConnectedUser(String externalUserId, long environmentId) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new TestConnectedUserAuthentication(externalUserId, environmentId));

        SecurityContextHolder.setContext(securityContext);
    }

    private ConnectedUser mockConnectedUser(long id, String externalId) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        lenient().when(connectedUser.getId())
            .thenReturn(id);
        lenient().when(connectedUser.getExternalId())
            .thenReturn(externalId);
        lenient().when(connectedUser.getEnvironmentId())
            .thenReturn(0L);

        when(connectedUserService.getConnectedUser(id)).thenReturn(connectedUser);

        return connectedUser;
    }

    private static final class TestConnectedUserAuthentication extends AbstractAuthenticationToken
        implements ConnectedUserAuthentication {

        private final String externalUserId;
        private final long environmentId;

        private TestConnectedUserAuthentication(String externalUserId, long environmentId) {
            super(List.of());

            this.externalUserId = externalUserId;
            this.environmentId = environmentId;

            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return externalUserId;
        }

        @Override
        public long connectedUserId() {
            return 0;
        }

        @Override
        public String externalUserId() {
            return externalUserId;
        }

        @Override
        public long environmentId() {
            return environmentId;
        }
    }
}

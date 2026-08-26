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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import java.util.Arrays;
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

    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 500L;
    private static final long DEVELOPMENT_INTEGRATION_INSTANCE_CONFIGURATION_ID = 501L;
    private static final long OTHER_INTEGRATION_INSTANCE_CONFIGURATION_ID = 502L;

    @Mock
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    private ConnectedUserConnectionFacadeImpl facade;

    /**
     * The membership collaborator is REAL, not a mock: what this facade lists is exactly what
     * {@code ConnectedUserResourceMembershipResolver} authorizes against, and a mocked union would let these tests pass
     * while the two disagree. Only the services underneath it are stubbed.
     */
    @BeforeEach
    void setUp() {
        ConnectedUserConnectionMembership connectedUserConnectionMembership = new ConnectedUserConnectionMembership(
            connectedUserConnectionService, integrationInstanceConfigurationWorkflowService,
            integrationInstanceService);

        facade = new ConnectedUserConnectionFacadeImpl(
            connectedUserConnectionMembership, connectedUserConnectionService, connectedUserService, connectionFacade);
    }

    /**
     * The connection-enumeration hole this facade used to have. {@code connectionIds} carries the host's
     * {@code sharedConnectionIds} by way of the browser, so it is a caller assertion the server cannot verify; it was
     * added to the lookup unconditionally, which let a connected user read any embedded connection in the tenant by
     * guessing its id. Restore the {@code allConnectionIds.addAll(connectionIds)} line and this test goes red.
     */
    @Test
    void testGetConnectionsIgnoresRequestedConnectionIdsTheUserDoesNotOwn() {
        ConnectedUser connectedUser = connectedUser();
        IntegrationInstance integrationInstance = integrationInstance(10L);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of(11L));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        facade.getConnections(1L, null, List.of(99L, 100L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue())
            .containsExactlyInAnyOrder(10L, 11L)
            .doesNotContain(99L, 100L);
    }

    /**
     * The dedup must not change what the caller receives. Whether or not this connected user has already been warned
     * about, every request still drops the ids they do not own -- the log is a migration signal, never the mechanism.
     */
    @Test
    void testGetConnectionsIgnoresUnownedIdsOnEveryCallNotJustTheFirst() {
        ConnectedUser connectedUser = connectedUser();
        IntegrationInstance integrationInstance = integrationInstance(10L);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of(11L));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        facade.getConnections(1L, null, List.of(99L));
        facade.getConnections(1L, null, List.of(99L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade, times(2)).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getAllValues())
            .allSatisfy(capturedConnectionIds -> assertThat(capturedConnectionIds)
                .containsExactlyInAnyOrder(10L, 11L)
                .doesNotContain(99L));
    }

    @Test
    void testGetConnectionsStillReturnsEveryOwnedConnection() {
        ConnectedUser connectedUser = connectedUser();
        IntegrationInstance integrationInstance = integrationInstance(10L);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of(11L));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        facade.getConnections(1L, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 11L);
    }

    /**
     * The case this ticket restores. A connection the tenant admin bound at the CONFIGURATION level is inherited by
     * every connected user whose integration instance derives from that configuration -- that is what a shared
     * connection is here -- so the picker must list it even though it is on neither the user's own instance nor their
     * own connections.
     */
    @Test
    void testGetConnectionsIncludesConnectionsBoundAtTheIntegrationInstanceConfiguration() {
        ConnectedUser connectedUser = connectedUser();

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance(10L)));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of(11L));
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(12L)));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        facade.getConnections(1L, null, List.of(12L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    /**
     * The other half of the rule: entitlement is derived from the configurations THIS caller's own instances derive
     * from, never from configurations at large. A configuration is shared by every connected user attached to it, so
     * enumerating configurations would hand each of them the others' connections.
     */
    @Test
    void testGetConnectionsExcludesConnectionsFromAConfigurationTheUserHasNoInstanceFor() {
        ConnectedUser connectedUser = connectedUser();

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance(10L)));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of());
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(12L)));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        facade.getConnections(1L, null, List.of(77L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).doesNotContain(77L);

        verify(integrationInstanceConfigurationWorkflowService)
            .getIntegrationInstanceConfigurationWorkflows(List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID));
    }

    /**
     * The configuration id set is derived from the caller's ENVIRONMENT-scoped instances, so a configuration in another
     * environment is never queried -- the door this ticket must not reopen. The instance lookup joins
     * {@code integration_instance_configuration.environment}, so a DEVELOPMENT caller's instances carry only
     * DEVELOPMENT configuration ids and the PRODUCTION twin's id never reaches the workflow lookup.
     */
    @Test
    void testGetConnectionsNeverWalksAConfigurationFromAnotherEnvironment() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(3L);
        when(connectedUser.getEnvironment()).thenReturn(Environment.DEVELOPMENT);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.DEVELOPMENT))
            .thenReturn(List.of(integrationInstance(10L, DEVELOPMENT_INTEGRATION_INSTANCE_CONFIGURATION_ID)));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of());
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(DEVELOPMENT_INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(12L)));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        facade.getConnections(1L, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 12L);

        verify(integrationInstanceService).getConnectedUserIntegrationInstances(3L, Environment.DEVELOPMENT);
        verify(integrationInstanceConfigurationWorkflowService, never())
            .getIntegrationInstanceConfigurationWorkflows(List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID));
    }

    /**
     * Entitlement is not ownership. A configuration-level connection is listed, but it belongs to the tenant admin who
     * bound it and is shared with every connected user on that configuration, so an end user must not be able to delete
     * or reauthorize it out from under the others.
     */
    @Test
    void testConfigurationLevelSharedConnectionIsListedButNotMutable() {
        ConnectedUser connectedUser = connectedUser(7L);

        when(connectedUserService.getConnectedUser(7L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(7L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance(10L)));
        when(connectedUserConnectionService.getConnectionIds(7L)).thenReturn(List.of());
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(12L)));

        // Echoes back only the ids it is asked for: requireOwned narrows the lookup to the OWNED subset, and a stub
        // that answered the same list regardless would hide exactly the narrowing this test is about.
        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.stream()
                    .map(connectionId -> ConnectionDTO.builder()
                        .id(connectionId)
                        .build())
                    .toList();
            });

        assertThat(facade.getConnections(7L, null, List.of()))
            .extracting(ConnectionDTO::id)
            .contains(12L);

        assertThrows(NoSuchElementException.class, () -> facade.deleteConnectedUserConnection(7L, 12L));

        verify(connectionFacade, never()).delete(any());
    }

    /**
     * getConnections no longer narrows the INSTANCE query by component name -- it queries the caller's instances for
     * the environment and lets the surviving exact-match filter on {@code connectionDTO.componentName()} do the work.
     * The two filters are on different columns ({@code integration.component_name} vs the connection's own component),
     * and the argument that the change is output-preserving rests on the removed one being strictly looser, so this
     * exercises the case that would expose it: a second instance belonging to a DIFFERENT integration, whose connection
     * must not appear in a component-scoped listing even though its configuration is walked for sharing.
     */
    @Test
    void testGetConnectionsFiltersOutAnotherIntegrationsConnectionByComponentName() {
        ConnectedUser connectedUser = connectedUser();

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(3L, Environment.PRODUCTION))
            .thenReturn(
                List.of(
                    integrationInstance(10L),
                    integrationInstance(20L, OTHER_INTEGRATION_INSTANCE_CONFIGURATION_ID)));
        when(connectedUserConnectionService.getConnectionIds(3L)).thenReturn(List.of());
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID, OTHER_INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(30L)));

        ConnectionDTO slackConnectionDTO = ConnectionDTO.builder()
            .id(10L)
            .componentName("slack")
            .build();
        ConnectionDTO hubspotConnectionDTO = ConnectionDTO.builder()
            .id(20L)
            .componentName("hubspot")
            .build();
        ConnectionDTO sharedHubspotConnectionDTO = ConnectionDTO.builder()
            .id(30L)
            .componentName("hubspot")
            .build();

        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenReturn(List.of(slackConnectionDTO, hubspotConnectionDTO, sharedHubspotConnectionDTO));

        assertThat(facade.getConnections(1L, "slack", List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactly(10L);

        assertThat(facade.getConnections(1L, "hubspot", List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactlyInAnyOrder(20L, 30L);
    }

    private static ConnectedUser connectedUser() {
        return connectedUser(3L);
    }

    private static ConnectedUser connectedUser(long connectedUserId) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(connectedUserId);
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        return connectedUser;
    }

    private static IntegrationInstance integrationInstance(long connectionId) {
        return integrationInstance(connectionId, INTEGRATION_INSTANCE_CONFIGURATION_ID);
    }

    private static IntegrationInstance integrationInstance(
        long connectionId, long integrationInstanceConfigurationId) {

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(connectionId);
        integrationInstance.setIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);

        return integrationInstance;
    }

    private static IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow(
        long... connectionIds) {

        List<IntegrationInstanceConfigurationWorkflowConnection> workflowConnections = Arrays.stream(connectionIds)
            .mapToObj(
                connectionId -> new IntegrationInstanceConfigurationWorkflowConnection(
                    connectionId, "connection", "node"))
            .toList();

        return new IntegrationInstanceConfigurationWorkflow(workflowConnections, Map.of(), "workflow-1");
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

        IntegrationInstance integrationInstance = integrationInstance(10L);

        when(integrationInstanceService.getConnectedUserIntegrationInstances(1L, Environment.PRODUCTION))
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

        verify(connectionFacade).replaceAuthorizationParameters(1L, parameters);
    }

    @Test
    void testReauthorizeRejectsForeignId() {
        stubConnectedUserOwnership(List.of(1L), List.of());
        stubOwnedConnectionDTOs(List.of(1L));

        Map<String, Object> parameters = Map.of("apiKey", "new");

        assertThrows(
            NoSuchElementException.class, () -> facade.reauthorizeConnectedUserConnection(7L, 9L, parameters));

        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
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
            .map(ConnectedUserConnectionFacadeTest::integrationInstance)
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

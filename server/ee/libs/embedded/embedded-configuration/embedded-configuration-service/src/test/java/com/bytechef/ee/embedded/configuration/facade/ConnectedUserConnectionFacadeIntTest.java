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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserConnectionFacadeIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserConnectionFacadeIntTest {

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationService integrationService;

    /**
     * {@code connectionIds} is a caller assertion the server cannot verify, so it may only narrow the connected user's
     * entitled set (own connections plus shared ones in their environment), never widen it. The entitled set is
     * resolved by the real {@code ConnectedUserConnectionMembership} over persisted connected_user_connection rows.
     */
    @Test
    void testGetConnectionsNarrowsToTheRequestedEntitledConnectionIds() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "narrowing-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        long connectedUserId = connectedUser.getId();
        long ownedConnectionAId = 81001L;
        long ownedConnectionBId = 81002L;
        long sharedConnectionId = 81003L;
        long unownedConnectionId = 81004L;

        connectedUserConnectionService.create(connectedUserId, ownedConnectionAId);
        connectedUserConnectionService.create(connectedUserId, ownedConnectionBId);

        Connection sharedConnection = new Connection();

        sharedConnection.setId(sharedConnectionId);

        when(connectionService.getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(sharedConnection));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.stream()
                    .map(connectionId -> ConnectionDTO.builder()
                        .componentName("slack")
                        .id(connectionId)
                        .build())
                    .toList();
            });

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactlyInAnyOrder(ownedConnectionAId, ownedConnectionBId, sharedConnectionId);

        assertThat(
            connectedUserConnectionFacade.getConnections(
                connectedUserId, null, List.of(ownedConnectionAId, unownedConnectionId)))
                    .extracting(ConnectionDTO::id)
                    .containsExactly(ownedConnectionAId);

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of(unownedConnectionId)))
            .isEmpty();
    }

    /**
     * The connection-enumeration hole this facade used to have. {@code connectionIds} carries the host's
     * {@code sharedConnectionIds} by way of the browser, so it is a caller assertion the server cannot verify; it was
     * added to the lookup unconditionally, which let a connected user read any embedded connection in the tenant by
     * guessing its id. {@code connectionIds} may now only narrow the entitled set, so ids the user does not own select
     * nothing.
     */
    @Test
    void testGetConnectionsNeverAddsRequestedConnectionIdsTheUserDoesNotOwn() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82010L, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUserId, 82011L);

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of(82099L, 82100L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).isEmpty();
    }

    /**
     * The dedup must not change what the caller receives. Whether or not this connected user has already been warned
     * about, every request still drops the ids they do not own -- the log is a migration signal, never the mechanism.
     */
    @Test
    void testGetConnectionsIgnoresUnownedIdsOnEveryCallNotJustTheFirst() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82110L, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUserId, 82111L);

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of(82199L));
        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of(82199L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade, times(2)).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getAllValues())
            .allSatisfy(capturedConnectionIds -> assertThat(capturedConnectionIds).isEmpty());
    }

    @Test
    void testGetConnectionsStillReturnsEveryOwnedConnection() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82210L, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUserId, 82211L);

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(82210L, 82211L);
    }

    /**
     * The case this ticket restores. A connection a tenant admin marked {@code shared} is entitled to every connected
     * user in the environment -- that is what a shared connection is now -- so the picker must list it even though it
     * is on neither the user's own instance nor their own connections.
     */
    @Test
    void testGetConnectionsIncludesConnectionsMarkedShared() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82310L, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUserId, 82311L);

        when(connectionService.getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(82312L)));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(82310L, 82311L, 82312L);
    }

    /**
     * The environment axis. Source 3 applies {@code environment.ordinal()} in its own query, so a shared connection
     * from another environment is never included -- the door this ticket must not reopen. The connected user also has
     * an instance under a PRODUCTION configuration, which the DEVELOPMENT-scoped instance lookup must leave out.
     */
    @Test
    void testGetConnectionsNeverIncludesASharedConnectionFromAnotherEnvironment() {
        long connectedUserId = createConnectedUser(Environment.DEVELOPMENT);

        createIntegrationInstance(connectedUserId, 82410L, Environment.DEVELOPMENT);
        createIntegrationInstance(connectedUserId, 82414L, Environment.PRODUCTION);

        when(connectionService.getSharedConnections(Environment.DEVELOPMENT.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(82412L)));
        when(connectionService.getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(82413L)));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(82410L, 82412L);

        verify(connectionService, never())
            .getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED);
    }

    /**
     * Entitlement is not ownership. A shared connection is listed, but it belongs to the tenant admin who marked it
     * shared and is entitled to every connected user in the environment, so an end user must not be able to delete or
     * reauthorize it out from under the others.
     */
    @Test
    void testSharedConnectionIsListedButNotMutable() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82510L, Environment.PRODUCTION);

        when(connectionService.getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(82512L)));

        // Echoes back only the ids it is asked for: requireOwned narrows the lookup to the OWNED subset, and a stub
        // that answered the same list regardless would hide exactly the narrowing this test is about.
        stubEchoingConnectionFacade();

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of()))
            .extracting(ConnectionDTO::id)
            .contains(82512L);

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUserId, 82512L));

        verify(connectionFacade, never()).delete(any());
    }

    /**
     * getConnections no longer narrows the INSTANCE query by component name -- it queries the caller's instances for
     * the environment and lets the surviving exact-match filter on {@code connectionDTO.componentName()} do the work.
     * The two filters are on different columns ({@code integration.component_name} vs the connection's own component),
     * and the argument that the change is output-preserving rests on the removed one being strictly looser, so this
     * exercises the case that would expose it: a second instance belonging to a DIFFERENT integration, whose connection
     * must not appear in a component-scoped listing even though a shared connection is present too.
     */
    @Test
    void testGetConnectionsFiltersOutAnotherIntegrationsConnectionByComponentName() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82610L, Environment.PRODUCTION);
        createIntegrationInstance(connectedUserId, 82620L, Environment.PRODUCTION);

        when(connectionService.getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(82630L)));

        ConnectionDTO slackConnectionDTO = ConnectionDTO.builder()
            .id(82610L)
            .componentName("slack")
            .build();
        ConnectionDTO hubspotConnectionDTO = ConnectionDTO.builder()
            .id(82620L)
            .componentName("hubspot")
            .build();
        ConnectionDTO sharedHubspotConnectionDTO = ConnectionDTO.builder()
            .id(82630L)
            .componentName("hubspot")
            .build();

        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenReturn(List.of(slackConnectionDTO, hubspotConnectionDTO, sharedHubspotConnectionDTO));

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, "slack", List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactly(82610L);

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, "hubspot", List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactlyInAnyOrder(82620L, 82630L);
    }

    @Test
    void testCreateConnectedUserConnection() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(82705L);

        long connectionId = connectedUserConnectionFacade.createConnectedUserConnection(connectedUserId, connectionDTO);

        assertThat(connectionId).isEqualTo(82705L);

        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserId)).containsExactly(82705L);
    }

    /**
     * The security boundary this facade owns. A connected user who could mark their own connection shared would hand
     * their credentials to every other connected user in the environment, so {@code shared} is forced off regardless of
     * what the request body carries.
     */
    @Test
    void testCreateConnectedUserConnectionForcesSharedFalse() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .name("My Slack")
            .shared(true)
            .build();

        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED))).thenReturn(82842L);

        connectedUserConnectionFacade.createConnectedUserConnection(connectedUserId, connectionDTO);

        ArgumentCaptor<ConnectionDTO> connectionDTOArgumentCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(connectionFacade).create(connectionDTOArgumentCaptor.capture(), eq(PlatformType.EMBEDDED));

        ConnectionDTO capturedConnectionDTO = connectionDTOArgumentCaptor.getValue();

        assertThat(capturedConnectionDTO.shared()).isFalse();
    }

    @Test
    void testGetConnectionsMergesInstanceAndConnectedUserConnectionIds() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82910L, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUserId, 82920L);

        when(connectionFacade.getConnections(List.of(82910L, 82920L), PlatformType.EMBEDDED)).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, "slack", List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactly(82910L, 82920L);
    }

    @Test
    void testGetConnectionsWithNullComponentNameReturnsAllOwnedConnections() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83001L, 83002L), List.of(83003L));

        ConnectionDTO slackConnection = ConnectionDTO.builder()
            .id(83001L)
            .componentName("slack")
            .build();
        ConnectionDTO hubspotConnection = ConnectionDTO.builder()
            .id(83002L)
            .componentName("hubspot")
            .build();
        ConnectionDTO githubConnection = ConnectionDTO.builder()
            .id(83003L)
            .componentName("github")
            .build();

        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenReturn(List.of(slackConnection, hubspotConnection, githubConnection));

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnections(
            connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(83001L, 83002L, 83003L);
        assertThat(connectionDTOs).containsExactlyInAnyOrder(slackConnection, hubspotConnection, githubConnection);
    }

    @Test
    void testDeleteConnectedUserConnectionRejectsForeignId() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83101L), List.of());
        stubOwnedConnectionDTOs(List.of(83101L));

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUserId, 83109L));

        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserId)).containsExactly(83101L);

        verify(connectionFacade, never()).delete(any());
    }

    /**
     * The connected-user link must be gone by the time the platform connection is deleted, so the stubbed
     * {@code ConnectionFacade#delete} reads the persisted rows at the moment it is called.
     */
    @Test
    void testDeleteConnectedUserConnectionDelegates() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83201L), List.of());
        stubOwnedConnectionDTOs(List.of(83201L));

        List<List<Long>> connectionIdsSeenOnDelete = new ArrayList<>();

        doAnswer(invocation -> {
            connectionIdsSeenOnDelete.add(connectedUserConnectionService.getConnectionIds(connectedUserId));

            return null;
        }).when(connectionFacade)
            .delete(83201L);

        connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUserId, 83201L);

        verify(connectionFacade).delete(83201L);

        assertThat(connectionIdsSeenOnDelete)
            .singleElement()
            .satisfies(connectionIds -> assertThat(connectionIds).isEmpty());
        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserId)).isEmpty();
    }

    @Test
    void testReauthorizeDelegates() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83301L), List.of());
        stubOwnedConnectionDTOs(List.of(83301L));

        Map<String, Object> parameters = Map.of("apiKey", "new");

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(connectedUserId, 83301L, parameters);

        verify(connectionFacade).replaceAuthorizationParameters(83301L, parameters);
    }

    @Test
    void testReauthorizeRejectsForeignId() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83401L), List.of());
        stubOwnedConnectionDTOs(List.of(83401L));

        Map<String, Object> parameters = Map.of("apiKey", "new");

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.reauthorizeConnectedUserConnection(connectedUserId, 83409L,
                parameters));

        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
    }

    /**
     * A shared connection is entitled -- {@code getConnections} must list it -- but never owned, since
     * {@code ConnectedUserConnectionMembership#getOwnedConnectionIds} structurally excludes source 3. The listing
     * assertion lives in this same test deliberately: it proves id 83550 genuinely went through source 3 rather than
     * merely resembling the pre-existing foreign-id case.
     */
    @Test
    void testDeleteConnectedUserConnectionRefusesSharedConnection() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        stubSharedConnection(83550L);

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of()))
            .extracting(ConnectionDTO::id)
            .contains(83550L);

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUserId, 83550L));

        verify(connectionFacade, never()).delete(any());
    }

    /**
     * The reauthorize counterpart of {@link #testDeleteConnectedUserConnectionRefusesSharedConnection}: the same
     * shared, entitled-but-unowned connection must be refused here too.
     */
    @Test
    void testReauthorizeConnectedUserConnectionRefusesSharedConnection() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        stubSharedConnection(83650L);

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of()))
            .extracting(ConnectionDTO::id)
            .contains(83650L);

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.reauthorizeConnectedUserConnection(connectedUserId, 83650L, Map.of()));

        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
    }

    private static Connection connection(long id) {
        Connection connection = new Connection();

        connection.setId(id);

        return connection;
    }

    private long createConnectedUser(Environment environment) {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "connected-user-" + UUID.randomUUID(), environment);

        return connectedUser.getId();
    }

    /**
     * Persists an integration instance for {@code connectedUserId} under its own integration and integration instance
     * configuration in {@code environment}, so each instance belongs to a different integration.
     */
    private void createIntegrationInstance(long connectedUserId, long connectionId, Environment environment) {
        String componentName = "integration-" + UUID.randomUUID();

        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName(componentName);

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(componentName);

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceService.create(connectedUserId, connectionId, integrationInstanceConfiguration.getId());
    }

    /**
     * Persists the connection-id sources that {@code requireOwned} walks for {@code connectedUserId}:
     * {@code connectedUserConnectionIds} as connected-user-linked connections and
     * {@code integrationInstanceConnectionIds} as the connections of PRODUCTION integration instances.
     */
    private void createOwnership(
        long connectedUserId, List<Long> connectedUserConnectionIds, List<Long> integrationInstanceConnectionIds) {

        for (long connectedUserConnectionId : connectedUserConnectionIds) {
            connectedUserConnectionService.create(connectedUserId, connectedUserConnectionId);
        }

        for (long integrationInstanceConnectionId : integrationInstanceConnectionIds) {
            createIntegrationInstance(connectedUserId, integrationInstanceConnectionId, Environment.PRODUCTION);
        }
    }

    /**
     * Makes {@code connectionFacade.getConnections} echo back one {@link ConnectionDTO} per requested id, so both the
     * entitled-ids lookup in {@code getConnections} and the owned-ids lookup in {@code requireOwned} resolve against
     * the ids they were actually called with, rather than a fixed canned list.
     */
    private void stubEchoingConnectionFacade() {
        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.stream()
                    .map(requestedConnectionId -> ConnectionDTO.builder()
                        .id(requestedConnectionId)
                        .build())
                    .toList();
            });
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

    /**
     * Marks connection {@code connectionId} shared for the PRODUCTION environment (source 3 of
     * {@code ConnectedUserConnectionMembership}) and makes {@code connectionFacade.getConnections} echo back the ids it
     * is asked for.
     */
    private void stubSharedConnection(long connectionId) {
        when(connectionService.getSharedConnections(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(connectionId)));

        stubEchoingConnectionFacade();
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}

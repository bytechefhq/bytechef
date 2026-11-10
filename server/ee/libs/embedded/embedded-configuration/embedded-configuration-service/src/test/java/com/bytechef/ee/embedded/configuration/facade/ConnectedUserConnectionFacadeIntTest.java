/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserSharedConnectionRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.constant.ConnectedUserConstants;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.component.domain.ConnectionDefinition;
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
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
        ConnectedUserConnectionFacadeIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.audit.enabled=true",
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
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
    private ConnectedUserConnectionRepository connectedUserConnectionRepository;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserSharedConnectionRepository connectedUserSharedConnectionRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

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

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private final Map<Long, Connection> connections = new HashMap<>();

    @BeforeEach
    void beforeEach() {
        when(connectionService.getConnections(anyList())).thenAnswer(invocation -> {
            List<Long> requestedConnectionIds = invocation.getArgument(0);

            return requestedConnectionIds.stream()
                .map(connections::get)
                .filter(Objects::nonNull)
                .toList();
        });
    }

    @AfterEach
    void afterEach() {
        connectedUserSharedConnectionRepository.deleteAll();
    }

    @Test
    void testGetConnectionsNarrowsToTheRequestedEntitledConnectionIds() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "narrowing-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        long connectedUserId = connectedUser.getId();
        long ownedConnectionAId = 81001L;
        long ownedConnectionBId = 81002L;
        long sharedConnectionId = 81003L;
        long unownedConnectionId = 81004L;

        createConnectedUserConnection(connectedUserId, ownedConnectionAId, Environment.PRODUCTION);
        createConnectedUserConnection(connectedUserId, ownedConnectionBId, Environment.PRODUCTION);

        stubSharedConnections(Environment.PRODUCTION, sharedConnectionId);
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

    @Test
    void testGetConnectionsNeverAddsRequestedConnectionIdsTheUserDoesNotOwn() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82010L, Environment.PRODUCTION);

        createConnectedUserConnection(connectedUserId, 82011L, Environment.PRODUCTION);

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of(82099L, 82100L));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testGetConnectionsIgnoresUnownedIdsOnEveryCallNotJustTheFirst() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82110L, Environment.PRODUCTION);

        createConnectedUserConnection(connectedUserId, 82111L, Environment.PRODUCTION);

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

        createConnectedUserConnection(connectedUserId, 82211L, Environment.PRODUCTION);

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(82210L, 82211L);
    }

    @Test
    void testGetConnectionsIncludesConnectionsMarkedShared() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82310L, Environment.PRODUCTION);

        createConnectedUserConnection(connectedUserId, 82311L, Environment.PRODUCTION);

        stubSharedConnections(Environment.PRODUCTION, 82312L);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(82310L, 82311L, 82312L);
    }

    @Test
    void testGetConnectionsNeverIncludesASharedConnectionFromAnotherEnvironment() {
        long connectedUserId = createConnectedUser(Environment.DEVELOPMENT);

        createIntegrationInstance(connectedUserId, 82410L, Environment.DEVELOPMENT);
        createIntegrationInstance(connectedUserId, 82414L, Environment.PRODUCTION);

        stubSharedConnections(Environment.DEVELOPMENT, 82412L);
        stubSharedConnections(Environment.PRODUCTION, 82413L);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenReturn(List.of());

        connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactlyInAnyOrder(82410L, 82412L);
    }

    @Test
    void testOwnedConnectionFromAnotherEnvironmentIsNeitherListedNorMutable() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82450L, Environment.PRODUCTION);
        createConnectedUserConnection(connectedUserId, 82451L, Environment.PRODUCTION);
        createConnectedUserConnection(connectedUserId, 82452L, Environment.DEVELOPMENT);

        stubEchoingConnectionFacade();

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactly(82450L, 82451L);
        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of(82452L)))
            .isEmpty();
        assertThat(connectedUserConnectionFacade.getOwnedConnectionIds(connectedUserId))
            .containsExactly(82450L, 82451L);

        Map<String, Object> parameters = Map.of("apiKey", "new");

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUserId, 82452L));
        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
                connectedUserId, 82452L, parameters));

        verify(connectionFacade, never()).delete(any());
        verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());

        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserId)).contains(82452L);
    }

    @Test
    void testSharedConnectionIsListedButNotMutable() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82510L, Environment.PRODUCTION);

        stubSharedConnections(Environment.PRODUCTION, 82512L);

        stubEchoingConnectionFacade();

        assertThat(connectedUserConnectionFacade.getConnections(connectedUserId, null, List.of()))
            .extracting(ConnectionDTO::id)
            .contains(82512L);

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUserId, 82512L));

        verify(connectionFacade, never()).delete(any());
    }

    @Test
    void testGetConnectionsFiltersOutAnotherIntegrationsConnectionByComponentName() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82610L, Environment.PRODUCTION);
        createIntegrationInstance(connectedUserId, 82620L, Environment.PRODUCTION);

        stubSharedConnections(Environment.PRODUCTION, 82630L);

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
        assertThat(findSingleConnectionEvent(ConnectionAuditEvents.CONNECTION_CREATED, 82705L).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId));
    }

    @Test
    void testCreateConnectedUserConnectionIsNeverShared() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .name("My Slack")
            .build();

        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED))).thenReturn(82842L);

        connectedUserConnectionFacade.createConnectedUserConnection(connectedUserId, connectionDTO);

        assertThat(connectedUserConnectionService.getSharedConnectionIds()).doesNotContain(82842L);
    }

    @Test
    void testGetConnectionsMergesInstanceAndConnectedUserConnectionIds() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 82910L, Environment.PRODUCTION);

        createConnectedUserConnection(connectedUserId, 82920L, Environment.PRODUCTION);

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
    void testGetConnectedUserConnectionsForAdminKeepsUserConnectionsWithoutCredentials() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createConnectedUserConnection(connectedUserId, 84001L, Environment.PRODUCTION);

        stubConnectionsWithCredentials();

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
            connectedUserId, null, List.of());

        assertThat(connectionDTOs).hasSize(1);

        ConnectionDTO connectionDTO = connectionDTOs.getFirst();

        assertThat(connectionDTO.id()).isEqualTo(84001L);
        assertThat(connectionDTO.authorizationParameters()).isNull();
        assertThat(connectionDTO.baseUri()).isNull();
        assertThat(connectionDTO.connectionParameters()).isNull();
        assertThat(connectionDTO.parameters()).isNull();
    }

    @Test
    void testGetConnectedUserConnectionsForAdminKeepsSharedConnectionCredentials() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        stubSharedConnection(84031L);

        stubConnectionsWithCredentials();

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
            connectedUserId, null, List.of());

        assertThat(connectionDTOs).hasSize(1);

        ConnectionDTO connectionDTO = connectionDTOs.getFirst();

        assertThat(connectionDTO.authorizationParameters()).isEqualTo(Map.of("secret", "value"));
    }

    @Test
    void testGetConnectedUserConnectionsForAdminKeepsAdminEnteredCredentials() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        registerConnection(84011L, Environment.PRODUCTION, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, 84011L, "admin@localhost.com");

        createProvisionedIntegrationInstance(connectedUserId, 84012L, Environment.PRODUCTION);

        stubConnectionsWithCredentials();

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
            connectedUserId, null, List.of());

        assertThat(connectionDTOs).hasSize(2);

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            assertThat(connectionDTO.authorizationParameters()).isEqualTo(Map.of("secret", "value"));
            assertThat(connectionDTO.baseUri()).isEqualTo("https://example.com");
            assertThat(connectionDTO.connectionParameters()).isEqualTo(Map.of("tenant", "acme"));
            assertThat(connectionDTO.parameters()).isEqualTo(Map.of("region", "eu"));
        }
    }

    @Test
    void testGetConnectedUserConnectionsKeepsCredentialsForTheConnectedUser() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createConnectedUserConnection(connectedUserId, 84021L, Environment.PRODUCTION);

        stubConnectionsWithCredentials();

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnectedUserConnections(
            connectedUserId, null, List.of());

        assertThat(connectionDTOs).hasSize(1);

        ConnectionDTO connectionDTO = connectionDTOs.getFirst();

        assertThat(connectionDTO.authorizationParameters()).isEqualTo(Map.of("secret", "value"));
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
        assertThat(findSingleConnectionEvent(ConnectionAuditEvents.CONNECTION_DELETED, 83201L).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId));
    }

    @Test
    void testReauthorizeDelegates() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83301L), List.of());
        stubOwnedConnectionDTOs(List.of(83301L));

        Map<String, Object> parameters = Map.of("apiKey", "new");

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(connectedUserId, 83301L, parameters);

        verify(connectionFacade).replaceAuthorizationParameters(83301L, parameters);

        assertThat(findSingleConnectionEvent(ConnectionAuditEvents.CONNECTION_REAUTHORIZED, 83301L).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId))
            .doesNotContainKey("parameters")
            .doesNotContainValue("new");
    }

    private static boolean hasConnectionId(PersistentAuditEvent persistentAuditEvent, long connectionId) {
        Map<String, String> data = persistentAuditEvent.getData();

        return String.valueOf(connectionId)
            .equals(data.get("connectionId"));
    }

    private PersistentAuditEvent findSingleConnectionEvent(String eventType, long connectionId) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 100));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .filter(persistentAuditEvent -> hasConnectionId(persistentAuditEvent, connectionId))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Test
    void testReauthorizeMergesTheIntegrationConfigurationCredentialsUnderTheNewParameters() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(
            connectedUserId, 83701L, Environment.PRODUCTION,
            Map.of("clientId", "app-id", "clientSecret", "app-secret"));
        stubOwnedConnectionDTOs(List.of(83701L));
        stubOAuth2ConnectionDefinition();

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
            connectedUserId, 83701L, Map.of("code", "fresh-code"));

        verify(connectionFacade).replaceAuthorizationParameters(
            83701L, Map.of("clientId", "app-id", "clientSecret", "app-secret", "code", "fresh-code"));
    }

    @Test
    void testReauthorizeLetsTheNewParametersWinOverTheIntegrationConfigurationCredentials() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(
            connectedUserId, 83711L, Environment.PRODUCTION,
            Map.of("clientId", "app-id", "clientSecret", "app-secret"));
        stubOwnedConnectionDTOs(List.of(83711L));
        stubOAuth2ConnectionDefinition();

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
            connectedUserId, 83711L, Map.of("clientSecret", "rotated-secret", "code", "fresh-code"));

        verify(connectionFacade).replaceAuthorizationParameters(
            83711L, Map.of("clientId", "app-id", "clientSecret", "rotated-secret", "code", "fresh-code"));
    }

    @Test
    void testReauthorizeRestoresOnlyTheAuthorizationParametersOfTheIntegrationConfiguration() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(
            connectedUserId, 83721L, Environment.PRODUCTION,
            Map.of("clientId", "app-id", "clientSecret", "app-secret", "region", "configuration-region"));
        stubOwnedConnectionDTOs(List.of(83721L));
        stubOAuth2ConnectionDefinition();

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
            connectedUserId, 83721L, Map.of("code", "fresh-code"));

        verify(connectionFacade).replaceAuthorizationParameters(
            83721L, Map.of("clientId", "app-id", "clientSecret", "app-secret", "code", "fresh-code"));
    }

    @Test
    void testUpdateIntegrationInstanceCredentialsKeepsConfigurationParameters() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(
            connectedUserId, 83731L, Environment.PRODUCTION,
            Map.of("clientId", "app-id", "clientSecret", "app-secret"), "admin@localhost.com");
        stubOwnedConnectionDTOs(List.of(83731L));

        ConnectionDefinition connectionDefinition = new ConnectionDefinition(
            ComponentDsl.connection()
                .authorizations(
                    ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                        .properties(
                            ComponentDsl.string("clientId"), ComponentDsl.string("clientSecret"),
                            ComponentDsl.string("token"))),
            "componentName", "Component", null);

        when(connectionDefinitionService.getConnectionConnectionDefinition(any(), anyInt()))
            .thenReturn(connectionDefinition);

        connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
            connectedUserId, 83731L, Map.of("token", "new"), 6);

        verify(connectionFacade).updateCredentials(
            83731L, Map.of("clientId", "app-id", "clientSecret", "app-secret", "token", "new"), 6);
    }

    @Test
    void testUpdateIntegrationInstanceCredentialsRejectsForeignConnection() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createOwnership(connectedUserId, List.of(83741L), List.of());
        stubOwnedConnectionDTOs(List.of(83741L));

        Map<String, Object> parameters = Map.of("token", "new");

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
                connectedUserId, 83749L, parameters, 0));

        verify(connectionFacade, never()).updateCredentials(anyLong(), any(), anyInt());
    }

    @Test
    void testUpdateIntegrationInstanceCredentialsRejectsSelfActivatedInstanceConnection() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 83751L, Environment.PRODUCTION);
        stubOwnedConnectionDTOs(List.of(83751L));

        Map<String, Object> parameters = Map.of("token", "new");

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
                connectedUserId, 83751L, parameters, 0));

        verify(connectionFacade, never()).updateCredentials(anyLong(), any(), anyInt());
    }

    @Test
    void testUpdateIntegrationInstanceCredentialsRejectsConnectionNotBackingAnInstance() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        registerConnection(83761L, Environment.PRODUCTION, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, 83761L, "admin@localhost.com");

        stubOwnedConnectionDTOs(List.of(83761L));

        Map<String, Object> parameters = Map.of("token", "new");

        assertThrows(
            NoSuchElementException.class,
            () -> connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
                connectedUserId, 83761L, parameters, 0));

        verify(connectionFacade, never()).updateCredentials(anyLong(), any(), anyInt());
    }

    @Test
    void testGetConnectedUserConnectionsForAdminStripsSelfActivatedInstanceCredentials() {
        long connectedUserId = createConnectedUser(Environment.PRODUCTION);

        createIntegrationInstance(connectedUserId, 84041L, Environment.PRODUCTION);
        createProvisionedIntegrationInstance(connectedUserId, 84042L, Environment.PRODUCTION);

        stubConnectionsWithCredentials();

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
            connectedUserId, null, List.of());

        assertThat(connectionDTOs)
            .extracting(ConnectionDTO::id)
            .containsExactlyInAnyOrder(84041L, 84042L);

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            if (Objects.equals(connectionDTO.id(), 84041L)) {
                assertThat(connectionDTO.authorizationParameters()).isNull();
                assertThat(connectionDTO.baseUri()).isNull();
                assertThat(connectionDTO.connectionParameters()).isNull();
                assertThat(connectionDTO.parameters()).isNull();
            } else {
                assertThat(connectionDTO.authorizationParameters()).isEqualTo(Map.of("secret", "value"));
            }
        }
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

    private void createConnectedUserConnection(long connectedUserId, long connectionId, Environment environment) {
        registerConnection(connectionId, environment, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);
    }

    private void createIntegrationInstance(long connectedUserId, long connectionId, Environment environment) {
        createIntegrationInstance(connectedUserId, connectionId, environment, Map.of());
    }

    private void createIntegrationInstance(
        long connectedUserId, long connectionId, Environment environment, Map<String, ?> connectionParameters) {

        createIntegrationInstance(connectedUserId, connectionId, environment, connectionParameters, null);
    }

    private void createIntegrationInstance(
        long connectedUserId, long connectionId, Environment environment, Map<String, ?> connectionParameters,
        @Nullable String provisionedBy) {

        registerConnection(connectionId, environment, PlatformType.EMBEDDED);

        String componentName = "integration-" + UUID.randomUUID();

        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName(componentName);

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(connectionParameters);
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(componentName);

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceService.create(
            connectedUserId, connectionId, integrationInstanceConfiguration.getId(), provisionedBy);
    }

    private void createProvisionedIntegrationInstance(
        long connectedUserId, long connectionId, Environment environment) {

        createIntegrationInstance(connectedUserId, connectionId, environment, Map.of(), "admin@localhost.com");
    }

    private void stubConnectionsWithCredentials() {
        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.stream()
                    .map(requestedConnectionId -> ConnectionDTO.builder()
                        .authorizationParameters(Map.of("secret", "value"))
                        .baseUri("https://example.com")
                        .connectionParameters(Map.of("tenant", "acme"))
                        .id(requestedConnectionId)
                        .parameters(Map.of("region", "eu"))
                        .build())
                    .toList();
            });
    }

    private void createOwnership(
        long connectedUserId, List<Long> connectedUserConnectionIds, List<Long> integrationInstanceConnectionIds) {

        for (long connectedUserConnectionId : connectedUserConnectionIds) {
            createConnectedUserConnection(connectedUserId, connectedUserConnectionId, Environment.PRODUCTION);
        }

        for (long integrationInstanceConnectionId : integrationInstanceConnectionIds) {
            createIntegrationInstance(connectedUserId, integrationInstanceConnectionId, Environment.PRODUCTION);
        }
    }

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

    private void stubOwnedConnectionDTOs(List<Long> ownedConnectionIds) {
        List<ConnectionDTO> ownedConnectionDTOs = ownedConnectionIds.stream()
            .map(connectionId -> ConnectionDTO.builder()
                .id(connectionId)
                .build())
            .toList();

        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED))).thenReturn(ownedConnectionDTOs);
    }

    private void stubOAuth2ConnectionDefinition() {
        ConnectionDefinition connectionDefinition = new ConnectionDefinition(
            ComponentDsl.connection()
                .authorizations(
                    ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                        .properties(ComponentDsl.string("clientId"), ComponentDsl.string("clientSecret")))
                .properties(ComponentDsl.string("region")),
            "componentName", "Component", null);

        when(connectionDefinitionService.getConnectionConnectionDefinition(any(), anyInt()))
            .thenReturn(connectionDefinition);
    }

    private void stubSharedConnection(long connectionId) {
        stubSharedConnections(Environment.PRODUCTION, connectionId);

        stubEchoingConnectionFacade();
    }

    private void stubSharedConnections(Environment environment, long... connectionIds) {
        for (long connectionId : connectionIds) {
            registerConnection(connectionId, environment, PlatformType.EMBEDDED);

            connectedUserConnectionService.updateShared(connectionId, true);
        }
    }

    private void registerConnection(long connectionId, Environment environment, PlatformType type) {
        Connection connection = connection(connectionId);

        connection.setEnvironmentId(environment.ordinal());
        connection.setType(type);

        connections.put(connectionId, connection);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class ConnectedUserServiceConfiguration {
    }

    @Nested
    @ContextConfiguration(classes = Authorization.MethodSecurityConfiguration.class)
    @MockitoBean(types = PermissionService.class)
    @MockitoSpyBean(types = ConnectedUserService.class)
    class Authorization {

        private static final long CONNECTED_USER_A_CONNECTION_ID = 91001L;
        private static final long CONNECTED_USER_B_CONNECTION_ID = 91002L;
        private static final long CONNECTION_ID = 91042L;
        private static final long PROVISIONED_CONNECTION_ID = 91043L;
        private static final long SELF_CREATED_CONNECTION_ID = 91044L;
        private static final long UNKNOWN_CONNECTED_USER_ID = Long.MAX_VALUE;

        @Autowired
        private PermissionService permissionService;

        @Autowired
        private TagService tagService;

        private long connectedUserADevelopmentId;
        private long connectedUserAId;
        private long connectedUserBId;
        private String externalUserAId;
        private String externalUserBId;

        @BeforeEach
        void beforeEach() {
            externalUserAId = "user-a-" + UUID.randomUUID();
            externalUserBId = "user-b-" + UUID.randomUUID();

            connectedUserAId = createConnectedUser(externalUserAId, Environment.PRODUCTION);
            connectedUserADevelopmentId = createConnectedUser(externalUserAId, Environment.DEVELOPMENT);
            connectedUserBId = createConnectedUser(externalUserBId, Environment.PRODUCTION);

            createConnectedUserConnection(connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, Environment.PRODUCTION);
            createConnectedUserConnection(connectedUserBId, CONNECTED_USER_B_CONNECTION_ID, Environment.PRODUCTION);

            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenReturn(CONNECTION_ID);
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
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testGetConnectedUserConnectionsDeniesConnectedUserOnAnotherConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.getConnectedUserConnections(connectedUserBId, null, List.of()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).getConnections(anyList(), any());
        }

        @Test
        void testGetConnectedUserConnectionsDeniesConnectedUserOnUnknownConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.getConnectedUserConnections(UNKNOWN_CONNECTED_USER_ID, null,
                    List.of()))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).getConnections(anyList(), any());
        }

        @Test
        void testGetConnectedUserConnectionsDeniesConnectedUserFromAnotherEnvironment() {
            authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.getConnectedUserConnections(connectedUserAId, null, List.of()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).getConnections(anyList(), any());
        }

        @Test
        void testGetConnectedUserConnectionsAllowsConnectedUserOnOwnConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThat(connectedUserConnectionFacade.getConnectedUserConnections(connectedUserAId, null, List.of()))
                .hasSize(1);

            verify(connectionFacade).getConnections(List.of(CONNECTED_USER_A_CONNECTION_ID), PlatformType.EMBEDDED);
        }

        @Test
        void testGetConnectedUserConnectionsAllowsTenantAdminOnAnyConnectedUser() {
            authenticate(createTenantAdminAuthentication());

            assertThat(connectedUserConnectionFacade.getConnectedUserConnections(connectedUserBId, null, List.of()))
                .hasSize(1);

            verify(connectionFacade).getConnections(List.of(CONNECTED_USER_B_CONNECTION_ID), PlatformType.EMBEDDED);
        }

        @Test
        void testGetConnectedUserConnectionsDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
            authenticate(createRegularUserAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.getConnectedUserConnections(connectedUserAId, null, List.of()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).getConnections(anyList(), any());
        }

        @Test
        void testCreateConnectedUserConnectionDeniesConnectedUserOnAnotherConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.createConnectedUserConnection(connectedUserBId, connectionDTO()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).create(any(), any());

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserBId))
                .containsExactly(CONNECTED_USER_B_CONNECTION_ID);
        }

        @Test
        void testCreateConnectedUserConnectionDeniesConnectedUserFromAnotherEnvironment() {
            authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.createConnectedUserConnection(connectedUserAId, connectionDTO()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).create(any(), any());

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserAId))
                .containsExactly(CONNECTED_USER_A_CONNECTION_ID);
        }

        @Test
        void testCreateConnectedUserConnectionAllowsConnectedUserOnOwnConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThat(connectedUserConnectionFacade.createConnectedUserConnection(connectedUserAId, connectionDTO()))
                .isEqualTo(CONNECTION_ID);

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserAId))
                .containsExactlyInAnyOrder(CONNECTED_USER_A_CONNECTION_ID, CONNECTION_ID);
        }

        @Test
        void testCreateConnectedUserConnectionAllowsTenantAdminOnAnyConnectedUser() {
            authenticate(createTenantAdminAuthentication());

            assertThat(connectedUserConnectionFacade.createConnectedUserConnection(connectedUserBId, connectionDTO()))
                .isEqualTo(CONNECTION_ID);

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserBId))
                .containsExactlyInAnyOrder(CONNECTED_USER_B_CONNECTION_ID, CONNECTION_ID);
        }

        @Test
        void testCreateConnectedUserConnectionAsAdminRecordsAdmin() {
            authenticate(createTenantAdminAuthentication());

            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenReturn(PROVISIONED_CONNECTION_ID);

            assertThat(
                connectedUserConnectionFacade.createConnectedUserConnectionAsAdmin(connectedUserBId, connectionDTO()))
                    .isEqualTo(PROVISIONED_CONNECTION_ID);

            assertThat(getProvisionedBy(PROVISIONED_CONNECTION_ID)).isEqualTo("admin@localhost.com");
            assertThat(getProvisionedBy(CONNECTED_USER_B_CONNECTION_ID)).isNull();
        }

        @Test
        void testCreateConnectedUserConnectionAsAdminHandsTheConnectionToTheConnectedUser() {
            authenticate(createTenantAdminAuthentication());

            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenReturn(PROVISIONED_CONNECTION_ID);

            connectedUserConnectionFacade.createConnectedUserConnectionAsAdmin(connectedUserBId, connectionDTO());

            verify(connectionService).updateCreatedBy(PROVISIONED_CONNECTION_ID, externalUserBId);

            assertThat(getProvisionedBy(PROVISIONED_CONNECTION_ID)).isEqualTo("admin@localhost.com");
        }

        @Test
        void testCreateConnectedUserConnectionAsAdminRejectsExternalIdLongerThanCreatedBy() {
            String longExternalId = "long-external-id-" + UUID.randomUUID();

            assertThat(longExternalId).hasSizeGreaterThan(ConnectedUserConstants.MAX_EXTERNAL_ID_LENGTH);

            long longExternalIdConnectedUserId = createConnectedUser(longExternalId, Environment.PRODUCTION);

            authenticate(createTenantAdminAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.createConnectedUserConnectionAsAdmin(
                    longExternalIdConnectedUserId, connectionDTO()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("longer than " + ConnectedUserConstants.MAX_EXTERNAL_ID_LENGTH);

            verify(connectionFacade, never()).create(any(), any());
        }

        @Test
        void testCreateConnectedUserConnectionLeavesCreatedByToTheConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenReturn(SELF_CREATED_CONNECTION_ID);

            connectedUserConnectionFacade.createConnectedUserConnection(connectedUserAId, connectionDTO());

            verify(connectionService, never()).updateCreatedBy(anyLong(), any());
        }

        @Test
        void testCreateConnectedUserConnectionAsAdminDeniesConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.createConnectedUserConnectionAsAdmin(
                    connectedUserAId, connectionDTO()))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).create(any(), any());
        }

        @Test
        void testCreateConnectedUserConnectionLeavesProvisionedByEmpty() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenReturn(SELF_CREATED_CONNECTION_ID);

            connectedUserConnectionFacade.createConnectedUserConnection(connectedUserAId, connectionDTO());

            assertThat(getProvisionedBy(SELF_CREATED_CONNECTION_ID)).isNull();
        }

        @Test
        void testCreateConnectedUserConnectionDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
            authenticate(createRegularUserAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.createConnectedUserConnection(connectedUserAId, connectionDTO()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).create(any(), any());

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserAId))
                .containsExactly(CONNECTED_USER_A_CONNECTION_ID);
        }

        @Test
        void testDeleteConnectedUserConnectionDeniesConnectedUserOnAnotherConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.deleteConnectedUserConnection(
                    connectedUserBId, CONNECTED_USER_B_CONNECTION_ID))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).delete(any());

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserBId))
                .containsExactly(CONNECTED_USER_B_CONNECTION_ID);
        }

        @Test
        void testDeleteConnectedUserConnectionDeniesConnectedUserFromAnotherEnvironment() {
            authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.deleteConnectedUserConnection(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).delete(any());

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserAId))
                .containsExactly(CONNECTED_USER_A_CONNECTION_ID);
        }

        @Test
        void testDeleteConnectedUserConnectionDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
            authenticate(createRegularUserAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.deleteConnectedUserConnection(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).delete(any());

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserAId))
                .containsExactly(CONNECTED_USER_A_CONNECTION_ID);
        }

        @Test
        void testDeleteConnectedUserConnectionAllowsConnectedUserOnOwnConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            connectedUserConnectionFacade.deleteConnectedUserConnection(
                connectedUserAId, CONNECTED_USER_A_CONNECTION_ID);

            verify(connectionFacade).delete(CONNECTED_USER_A_CONNECTION_ID);

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserAId)).isEmpty();
        }

        @Test
        void testDeleteConnectedUserConnectionAllowsTenantAdminOnAnyConnectedUser() {
            authenticate(createTenantAdminAuthentication());

            connectedUserConnectionFacade.deleteConnectedUserConnection(
                connectedUserBId, CONNECTED_USER_B_CONNECTION_ID);

            verify(connectionFacade).delete(CONNECTED_USER_B_CONNECTION_ID);

            assertThat(connectedUserConnectionService.getConnectionIds(connectedUserBId)).isEmpty();
        }

        @Test
        void testReauthorizeConnectedUserConnectionDeniesConnectedUserOnAnotherConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
                    connectedUserBId, CONNECTED_USER_B_CONNECTION_ID, Map.of("apiKey", "new")))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
        }

        @Test
        void testReauthorizeConnectedUserConnectionDeniesConnectedUserFromAnotherEnvironment() {
            authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, Map.of("apiKey", "new")))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
        }

        @Test
        void testReauthorizeConnectedUserConnectionDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
            authenticate(createRegularUserAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, Map.of("apiKey", "new")))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).replaceAuthorizationParameters(anyLong(), any());
        }

        @Test
        void testUpdateIntegrationInstanceConnectionCredentialsDeniesConnectedUserAndNonAdmin() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, Map.of("token", "new"), 0))
                        .isInstanceOf(AccessDeniedException.class);

            authenticate(createRegularUserAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.updateIntegrationInstanceConnectionCredentials(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, Map.of("token", "new"), 0))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).updateCredentials(anyLong(), any(), anyInt());
        }

        @Test
        void testReauthorizeConnectedUserConnectionAllowsConnectedUserOnOwnConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            Map<String, Object> parameters = Map.of("apiKey", "new");

            connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
                connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, parameters);

            verify(connectionFacade).replaceAuthorizationParameters(CONNECTED_USER_A_CONNECTION_ID, parameters);
        }

        @Test
        void testReauthorizeConnectedUserConnectionAllowsTenantAdminOnAnyConnectedUser() {
            authenticate(createTenantAdminAuthentication());

            Map<String, Object> parameters = Map.of("apiKey", "new");

            connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
                connectedUserBId, CONNECTED_USER_B_CONNECTION_ID, parameters);

            verify(connectionFacade).replaceAuthorizationParameters(CONNECTED_USER_B_CONNECTION_ID, parameters);
        }

        @Test
        void testGetConnectedUserConnectionsByExternalUserIdAllowsTheMatchingPrincipal() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThat(
                connectedUserConnectionFacade.getConnectedUserConnections(
                    externalUserAId, Environment.PRODUCTION, null, List.of()))
                        .hasSize(1);
        }

        @Test
        void testGetConnectedUserConnectionsByExternalUserIdDeniesAnotherExternalUserId() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.getConnectedUserConnections(
                    externalUserBId, Environment.PRODUCTION, null, List.of()))
                        .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> connectedUserConnectionFacade.getOwnedConnectionIds(externalUserBId, Environment.PRODUCTION))
                    .isInstanceOf(AccessDeniedException.class);

            verify(connectedUserService, never()).getConnectedUser(externalUserBId, Environment.PRODUCTION);
        }

        @Test
        void testGetConnectedUserConnectionTagsListsOnlyTheConnectedUsersOwnConnectionTags() {
            long sharedConnectionId = 91045L;

            stubSharedConnections(Environment.PRODUCTION, sharedConnectionId);

            Tag zetaTag = tagService.create(new Tag("zeta-" + UUID.randomUUID()));
            Tag alphaTag = tagService.create(new Tag("alpha-" + UUID.randomUUID()));
            Tag otherUserTag = tagService.create(new Tag("other-" + UUID.randomUUID()));
            Tag sharedTag = tagService.create(new Tag("shared-" + UUID.randomUUID()));

            setTagIds(CONNECTED_USER_A_CONNECTION_ID, zetaTag, alphaTag);
            setTagIds(CONNECTED_USER_B_CONNECTION_ID, otherUserTag);
            setTagIds(sharedConnectionId, sharedTag);

            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThat(connectedUserConnectionFacade.getConnectedUserConnectionTags(connectedUserAId))
                .extracting(Tag::getName)
                .containsExactly(alphaTag.getName(), zetaTag.getName());
        }

        @Test
        void testGetConnectedUserConnectionTagsDeniesConnectedUserOnAnotherConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(() -> connectedUserConnectionFacade.getConnectedUserConnectionTags(connectedUserBId))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateConnectedUserConnectionTagsUpdatesTheConnectedUsersOwnConnection() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            List<Tag> tags = List.of(new Tag("tag1"));

            connectedUserConnectionFacade.updateConnectedUserConnectionTags(
                connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, tags);

            verify(connectionFacade).update(CONNECTED_USER_A_CONNECTION_ID, tags);
        }

        @Test
        void testUpdateConnectedUserConnectionTagsRejectsAConnectionTheConnectedUserDoesNotOwn() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.updateConnectedUserConnectionTags(
                    connectedUserAId, CONNECTED_USER_B_CONNECTION_ID, List.of(new Tag("tag1"))))
                        .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).update(anyLong(), anyList());
        }

        @Test
        void testUpdateConnectedUserConnectionTagsDeniesConnectedUserOnAnotherConnectedUser() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.updateConnectedUserConnectionTags(
                    connectedUserBId, CONNECTED_USER_B_CONNECTION_ID, List.of(new Tag("tag1"))))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).update(anyLong(), anyList());
        }

        @Test
        void testGetConnectedUserConnectionTagsDeniesTenantAdmin() {
            authenticate(createTenantAdminAuthentication());

            assertThatThrownBy(() -> connectedUserConnectionFacade.getConnectedUserConnectionTags(connectedUserAId))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateConnectedUserConnectionTagsDeniesTenantAdmin() {
            authenticate(createTenantAdminAuthentication());

            assertThatThrownBy(
                () -> connectedUserConnectionFacade.updateConnectedUserConnectionTags(
                    connectedUserAId, CONNECTED_USER_A_CONNECTION_ID, List.of(new Tag("tag1"))))
                        .isInstanceOf(AccessDeniedException.class);

            verify(connectionFacade, never()).update(anyLong(), anyList());
        }

        private void setTagIds(long connectionId, Tag... tags) {
            Connection connection = connections.get(connectionId);

            connection.setTagIds(
                Arrays.stream(tags)
                    .map(Tag::getId)
                    .toList());
        }

        private String getProvisionedBy(long connectionId) {
            ConnectedUserConnection connectedUserConnection =
                connectedUserConnectionRepository.findAll()
                    .stream()
                    .filter(curConnectedUserConnection -> curConnectedUserConnection.getConnectionId() == connectionId)
                    .findFirst()
                    .orElseThrow();

            return connectedUserConnection.getProvisionedBy();
        }

        private static ConnectionDTO connectionDTO() {
            return ConnectionDTO.builder()
                .componentName("slack")
                .build();
        }

        private static void authenticate(Authentication authentication) {
            SecurityContextHolder.getContext()
                .setAuthentication(authentication);
        }

        private EmbeddedApiKeyAuthenticationToken createConnectedUserAuthentication(Environment environment) {
            long connectedUserId =
                environment == Environment.PRODUCTION ? connectedUserAId : connectedUserADevelopmentId;

            return new EmbeddedApiKeyAuthenticationToken(
                environment.ordinal(), connectedUserId, new User(externalUserAId, "", List.of()), false);
        }

        private long createConnectedUser(String externalUserId, Environment environment) {
            ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalUserId, environment);

            return connectedUser.getId();
        }

        private static Authentication createRegularUserAuthentication() {
            return new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        }

        private Authentication createTenantAdminAuthentication() {
            when(permissionService.isTenantAdmin()).thenReturn(true);

            return new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Configuration
        @EnableMethodSecurity
        static class MethodSecurityConfiguration {

            @Bean
            static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
                @Lazy PermissionService permissionService,
                ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

                return new AutomationMethodSecurityExpressionHandler(
                    permissionService, methodSecurityExpressionContributorProvider);
            }
        }
    }
}

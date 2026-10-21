/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationConnectionWorkflowDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, ConnectionFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "bytechef.audit.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectionFacadeIntTest {

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private com.bytechef.platform.connection.facade.ConnectionFacade platformConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @MockitoBean
    private ConnectedUserConnectionService connectedUserConnectionService;

    @MockitoBean
    private ConnectedUserProjectService connectedUserProjectService;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private TagService tagService;

    @MockitoBean
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsDeniedEveryOperation() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertEveryOperationIsDenied();
    }

    @Test
    void testNonAdminIsDeniedEveryOperation() {
        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertEveryOperationIsDenied();
    }

    @Test
    void testTenantAdminReachesTheSharedFacadeTypedEmbedded() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .name("Team Slack")
            .build();

        grantEveryEmbeddedScope();

        when(platformConnectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(7L);
        doAnswer(invocation -> {
            Runnable releaseReferences = invocation.getArgument(1);

            releaseReferences.run();

            return null;
        }).when(platformConnectionFacade)
            .delete(eq(7L), any());
        when(connectionService.fetchConnection(7L)).thenReturn(Optional.of(connection(7L, PlatformType.EMBEDDED)));
        when(platformConnectionFacade.getConnection(7L)).thenReturn(connectionDTO);

        assertThat(connectionFacade.createConnection(connectionDTO, true)).isEqualTo(7L);

        connectionFacade.deleteConnection(7L);
        connectionFacade.getConnection(7L);
        connectionFacade.getConnectionTags();
        connectionFacade.getConnections("slack", 1, 2L, 3L);
        connectionFacade.getSharedConnectionIds();
        connectionFacade.updateConnection(7L, "name", List.of(), true, 0);
        connectionFacade.updateConnectionCredentials(7L, Map.of("token", "secret"), 0);
        connectionFacade.updateConnectionTags(7L, List.of());

        verify(platformConnectionFacade).updateCredentials(7L, Map.of("token", "secret"), 0);
        verify(platformConnectionFacade).delete(eq(7L), any());
        verify(platformConnectionFacade).getConnection(7L);
        verify(connectionService).getConnections(PlatformType.EMBEDDED);
        verify(platformConnectionFacade).getConnections("slack", 1, List.of(), 3L, 2L, PlatformType.EMBEDDED);
        verify(platformConnectionFacade).update(7L, "name", List.of(), 0);
        verify(platformConnectionFacade).update(7L, List.of());
        verify(connectedUserConnectionService, times(2)).updateShared(7L, true);
        verify(connectedUserConnectionService).deleteByConnectionId(7L);
        verify(connectedUserConnectionService).getSharedConnectionIds();

        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_CREATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", "7")
            .containsEntry("name", "Team Slack")
            .containsEntry("componentName", "slack")
            .containsEntry("shared", "true");
        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", "7");
        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", "7")
            .containsEntry("name", "name")
            .containsEntry("shared", "true");

        PersistentAuditEvent credentialsUpdatedEvent =
            findSingleEvent(ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED);

        assertThat(credentialsUpdatedEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", "7");

        assertNoDataValueContains(credentialsUpdatedEvent, "secret");
    }

    private void assertNoDataValueContains(PersistentAuditEvent persistentAuditEvent, String text) {
        assertThat(persistentAuditEvent.getData()
            .values()).noneMatch(
                value -> String.valueOf(value)
                    .contains(text));
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 100));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private void assertEveryOperationIsDenied() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .build();

        assertThatThrownBy(() -> connectionFacade.createConnection(connectionDTO, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.deleteConnection(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.getConnection(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.getConnectionTags())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.getConnections(null, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.getSharedConnectionIds())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.updateConnection(7L, "name", List.of(), null, 0))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionFacade.updateConnectionTags(7L, List.of()))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(platformConnectionFacade, connectionService, connectedUserConnectionService);
    }

    private void grantEveryEmbeddedScope() {
        when(permissionService.hasResourceScope(anyLong(), eq("Connection"), anyString())).thenReturn(true);
        when(permissionService.hasEmbeddedScope(anyString())).thenReturn(true);
        when(permissionService.hasEmbeddedScope(anyString(), any(Environment.class))).thenReturn(true);
        when(permissionService.hasEmbeddedScopeInEveryEnvironment(anyString())).thenReturn(true);
    }

    private static Connection connection(long id, PlatformType type) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setType(type);

        return connection;
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    @Nested
    class EmbeddedTypeGuard {

        private static final long AUTOMATION_CONNECTION_ID = 8L;
        private static final long UNKNOWN_CONNECTION_ID = 9L;

        @BeforeEach
        void beforeEach() {
            authenticate(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            grantEveryEmbeddedScope();

            when(connectionService.fetchConnection(AUTOMATION_CONNECTION_ID))
                .thenReturn(Optional.of(connection(AUTOMATION_CONNECTION_ID, PlatformType.AUTOMATION)));
            when(connectionService.fetchConnection(UNKNOWN_CONNECTION_ID)).thenReturn(Optional.empty());
        }

        @Test
        void testDeleteConnectionRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionFacade.deleteConnection(AUTOMATION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).delete(anyLong(), any());
            verify(connectedUserConnectionService, never()).deleteByConnectionId(anyLong());

            assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_DELETED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", NoSuchElementException.class.getName())
                .containsEntry("connectionId", String.valueOf(AUTOMATION_CONNECTION_ID));
        }

        @Test
        void testGetConnectionRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionFacade.getConnection(AUTOMATION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testUpdateConnectionRejectsAutomationConnection() {
            assertThatThrownBy(
                () -> connectionFacade.updateConnection(AUTOMATION_CONNECTION_ID, "name", List.of(), true, 0))
                    .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).update(anyLong(), anyString(), any(), anyInt());
            verify(connectedUserConnectionService, never()).updateShared(anyLong(), anyBoolean());
        }

        @Test
        void testUpdateConnectionTagsRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionFacade.updateConnectionTags(AUTOMATION_CONNECTION_ID, List.of()))
                .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).update(anyLong(), anyList());
        }

        @Test
        void testUnknownConnectionIsRejectedLikeAnAutomationConnection() {
            assertThatThrownBy(() -> connectionFacade.deleteConnection(UNKNOWN_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Connection id=%s not found".formatted(UNKNOWN_CONNECTION_ID));
            assertThatThrownBy(() -> connectionFacade.getConnection(AUTOMATION_CONNECTION_ID))
                .hasMessage("Connection id=%s not found".formatted(AUTOMATION_CONNECTION_ID));

            verify(platformConnectionFacade, never()).delete(anyLong(), any());
        }
    }

    @Nested
    class ConnectedUserOwnership {

        private static final long ADMIN_CONNECTION_ID = 1L;
        private static final long CONNECTED_USER_CONNECTION_ID = 2L;
        private static final long INTEGRATION_INSTANCE_CONNECTION_ID = 3L;
        private static final long SHARED_CONNECTION_ID = 4L;

        @BeforeEach
        void beforeEach() {
            authenticate(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            grantEveryEmbeddedScope();

            when(connectedUserConnectionService.getConnectionIdsIn(anySet())).thenAnswer(invocation -> {
                Set<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.contains(CONNECTED_USER_CONNECTION_ID)
                    ? Set.of(CONNECTED_USER_CONNECTION_ID) : Set.of();
            });
            when(connectedUserConnectionService.existsByConnectionId(CONNECTED_USER_CONNECTION_ID)).thenReturn(true);
            when(integrationInstanceService.getConnectionIdsIn(anySet())).thenAnswer(invocation -> {
                Set<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.contains(INTEGRATION_INSTANCE_CONNECTION_ID)
                    ? Set.of(INTEGRATION_INSTANCE_CONNECTION_ID) : Set.of();
            });
            when(integrationInstanceService.existsByConnectionId(INTEGRATION_INSTANCE_CONNECTION_ID)).thenReturn(true);
        }

        @Test
        void testGetConnectionsExcludesIntegrationInstanceConnections() {
            stubConnections(
                connectionDTO(ADMIN_CONNECTION_ID, List.of()),
                connectionDTO(INTEGRATION_INSTANCE_CONNECTION_ID, List.of()));

            List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(null, null, null, null);

            assertThat(connectionDTOs).extracting(ConnectionDTO::id)
                .containsExactly(ADMIN_CONNECTION_ID);
        }

        @Test
        void testGetConnectionThrowsForIntegrationInstanceConnection() {
            stubEmbedded(INTEGRATION_INSTANCE_CONNECTION_ID);

            assertThatThrownBy(() -> connectionFacade.getConnection(INTEGRATION_INSTANCE_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testGetConnectionsExcludesConnectedUserConnections() {
            stubConnections(
                connectionDTO(ADMIN_CONNECTION_ID, List.of()), connectionDTO(CONNECTED_USER_CONNECTION_ID, List.of()));

            List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(null, null, null, null);

            assertThat(connectionDTOs).extracting(ConnectionDTO::id)
                .containsExactly(ADMIN_CONNECTION_ID);
        }

        @Test
        void testGetConnectionsKeepsSharedAdminConnections() {
            stubConnections(
                connectionDTO(ADMIN_CONNECTION_ID, List.of()), connectionDTO(SHARED_CONNECTION_ID, List.of()));

            when(connectedUserConnectionService.getSharedConnectionIds()).thenReturn(Set.of(SHARED_CONNECTION_ID));

            List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(null, null, null, null);

            assertThat(connectionDTOs).extracting(ConnectionDTO::id)
                .containsExactly(ADMIN_CONNECTION_ID, SHARED_CONNECTION_ID);
        }

        @Test
        void testGetConnectionThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(() -> connectionFacade.getConnection(CONNECTED_USER_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testUpdateConnectionThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(
                () -> connectionFacade.updateConnection(
                    CONNECTED_USER_CONNECTION_ID, "name", List.of(), null, 0))
                        .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).update(anyLong(), anyString(), any(), anyInt());
        }

        @Test
        void testDeleteConnectionThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(() -> connectionFacade.deleteConnection(CONNECTED_USER_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).delete(anyLong(), any());
            verify(connectedUserConnectionService, never()).deleteByConnectionId(anyLong());
        }

        @Test
        void testUpdateConnectionCredentialsDelegatesForAdminConnection() {
            stubEmbedded(ADMIN_CONNECTION_ID);

            Map<String, Object> parameters = Map.of("token", "secret");

            connectionFacade.updateConnectionCredentials(ADMIN_CONNECTION_ID, parameters, 2);

            verify(platformConnectionFacade).updateCredentials(ADMIN_CONNECTION_ID, parameters, 2);
        }

        @Test
        void testUpdateConnectionCredentialsThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            Map<String, String> parameters = Map.of("token", "secret");

            assertThatThrownBy(
                () -> connectionFacade.updateConnectionCredentials(
                    CONNECTED_USER_CONNECTION_ID, parameters, 0))
                        .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).updateCredentials(anyLong(), any(), anyInt());
        }

        @Test
        void testUpdateConnectionTagsThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(
                () -> connectionFacade.updateConnectionTags(CONNECTED_USER_CONNECTION_ID, List.of()))
                    .isInstanceOf(NoSuchElementException.class);

            verify(platformConnectionFacade, never()).update(anyLong(), anyList());
        }

        @Test
        void testUpdateConnectionTagsUpdatesAdminConnection() {
            stubEmbedded(ADMIN_CONNECTION_ID);

            List<Tag> tags = List.of(new Tag(11L, "admin"));

            connectionFacade.updateConnectionTags(ADMIN_CONNECTION_ID, tags);

            verify(platformConnectionFacade).update(ADMIN_CONNECTION_ID, tags);
        }

        @Test
        void testGetConnectionTagsReturnsOnlyAdminConnectionTagsSortedByName() {
            Tag zetaTag = tagService.create(new Tag("zeta-" + UUID.randomUUID()));
            Tag alphaTag = tagService.create(new Tag("alpha-" + UUID.randomUUID()));
            Tag customerTag = tagService.create(new Tag("customer-" + UUID.randomUUID()));

            Connection adminConnection = connection(ADMIN_CONNECTION_ID, PlatformType.EMBEDDED);

            adminConnection.setTagIds(List.of(zetaTag.getId(), alphaTag.getId()));

            Connection sharedConnection = connection(SHARED_CONNECTION_ID, PlatformType.EMBEDDED);

            sharedConnection.setTagIds(List.of(alphaTag.getId()));

            Connection connectedUserConnection = connection(CONNECTED_USER_CONNECTION_ID, PlatformType.EMBEDDED);

            connectedUserConnection.setTagIds(List.of(customerTag.getId()));

            when(connectionService.getConnections(PlatformType.EMBEDDED))
                .thenReturn(List.of(adminConnection, sharedConnection, connectedUserConnection));

            assertThat(connectionFacade.getConnectionTags())
                .extracting(Tag::getName)
                .containsExactly(alphaTag.getName(), zetaTag.getName());

            verify(platformConnectionFacade, never()).getConnections(any(), any(), anyList(), any(), any(), any());
        }

        private ConnectionDTO connectionDTO(long id, List<Tag> tags) {
            return ConnectionDTO.builder()
                .id(id)
                .tags(tags)
                .build();
        }

        private void stubConnections(ConnectionDTO... connectionDTOs) {
            when(
                platformConnectionFacade.getConnections(any(), any(), anyList(), any(), any(), any(PlatformType.class)))
                    .thenReturn(List.of(connectionDTOs));
        }

        private void stubEmbedded(long id) {
            when(connectionService.fetchConnection(id)).thenReturn(Optional.of(connection(id, PlatformType.EMBEDDED)));
        }
    }

    @Nested
    class ConnectionWorkflows {

        private static final long CONNECTION_ID = 8080L;

        private final List<ConnectedUser> connectedUsers = new ArrayList<>();
        private final List<Workflow> workflows = new ArrayList<>();

        @Autowired
        private ConnectedUserService connectedUserService;

        @Autowired
        private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

        @Autowired
        private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

        @Autowired
        private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

        @Autowired
        private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

        @Autowired
        private IntegrationRepository integrationRepository;

        @Autowired
        private IntegrationService integrationService;

        @Autowired
        private IntegrationWorkflowRepository integrationWorkflowRepository;

        @Autowired
        private IntegrationWorkflowService integrationWorkflowService;

        @Autowired
        private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

        @Autowired
        private ProjectWorkflowService projectWorkflowService;

        @Autowired
        private WorkflowService workflowService;

        @Autowired
        private WorkflowTestConfigurationService workflowTestConfigurationService;

        @BeforeEach
        void beforeEach() {
            authenticate(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            grantEveryEmbeddedScope();

            when(permissionService.isTenantAdmin()).thenReturn(true);
            when(connectionService.fetchConnection(CONNECTION_ID))
                .thenReturn(Optional.of(connection(CONNECTION_ID, PlatformType.EMBEDDED)));
            when(connectedUserService.getConnectedUsers(anyList()))
                .thenAnswer(invocation -> filterById(connectedUsers, invocation.getArgument(0), ConnectedUser::getId));
            when(workflowService.getWorkflows(anyList()))
                .thenAnswer(invocation -> filterById(workflows, invocation.getArgument(0), Workflow::getId));
        }

        @AfterEach
        void afterEach() {
            integrationInstanceConfigurationWorkflowRepository.deleteAll();
            integrationInstanceConfigurationRepository.deleteAll();
            integrationWorkflowRepository.deleteAll();
            integrationRepository.deleteAll();
        }

        @Test
        void testGetConnectionWorkflowsListsDeployedAndEditorIntegrationWorkflows() {
            Integration integration = createIntegration("HubSpot Sync");

            Workflow deployedWorkflow = addWorkflow(integration, "Push contacts");
            Workflow editorWorkflow = addWorkflow(integration, "Pull deals");

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                createIntegrationInstanceConfigurationWorkflow(integration, deployedWorkflow, CONNECTION_ID);

            createIntegrationInstanceConfigurationWorkflow(integration, editorWorkflow, 9090L);

            WorkflowTestConfiguration editorTestConfiguration = workflowTestConfiguration(77L, editorWorkflow.getId());

            when(workflowTestConfigurationService.getConnectionWorkflowTestConfigurations(CONNECTION_ID))
                .thenReturn(
                    List.of(editorTestConfiguration, workflowTestConfiguration(78L, "workflow-outside-integration")));

            IntegrationWorkflow deployedIntegrationWorkflow =
                integrationWorkflowService.getWorkflowIntegrationWorkflow(deployedWorkflow.getId());
            IntegrationWorkflow editorIntegrationWorkflow =
                integrationWorkflowService.getWorkflowIntegrationWorkflow(editorWorkflow.getId());

            List<IntegrationConnectionWorkflowDTO> integrationConnectionWorkflowDTOs =
                connectionFacade.getConnectionWorkflows(CONNECTION_ID);

            assertThat(integrationConnectionWorkflowDTOs)
                .extracting(
                    IntegrationConnectionWorkflowDTO::id, IntegrationConnectionWorkflowDTO::parentId,
                    IntegrationConnectionWorkflowDTO::parentName, IntegrationConnectionWorkflowDTO::parentType,
                    IntegrationConnectionWorkflowDTO::parentVersion, IntegrationConnectionWorkflowDTO::usage,
                    IntegrationConnectionWorkflowDTO::workflowId, IntegrationConnectionWorkflowDTO::workflowName,
                    IntegrationConnectionWorkflowDTO::workflowUuid)
                .containsExactly(
                    tuple(
                        77L, integration.getId(), "HubSpot Sync",
                        IntegrationConnectionWorkflowDTO.ParentType.INTEGRATION,
                        1, IntegrationConnectionWorkflowDTO.Usage.EDITOR, editorWorkflow.getId(), "Pull deals",
                        editorIntegrationWorkflow.getUuidAsString()),
                    tuple(
                        integrationInstanceConfigurationWorkflow.getId(), integration.getId(), "HubSpot Sync",
                        IntegrationConnectionWorkflowDTO.ParentType.INTEGRATION, 1,
                        IntegrationConnectionWorkflowDTO.Usage.DEPLOYMENT, deployedWorkflow.getId(), "Push contacts",
                        deployedIntegrationWorkflow.getUuidAsString()));
        }

        @Test
        void testGetConnectionWorkflowsListsEditorWorkflowsOfConnectedUserProjects() {
            Workflow namedUserWorkflow = workflow("Route invoice emails");
            Workflow unnamedUserWorkflow = workflow("Archive receipts");
            Workflow anonymousUserWorkflow = workflow("Tag expenses");

            when(workflowTestConfigurationService.getConnectionWorkflowTestConfigurations(CONNECTION_ID))
                .thenReturn(
                    List.of(
                        workflowTestConfiguration(81L, namedUserWorkflow.getId()),
                        workflowTestConfiguration(82L, namedUserWorkflow.getId()),
                        workflowTestConfiguration(83L, unnamedUserWorkflow.getId()),
                        workflowTestConfiguration(85L, anonymousUserWorkflow.getId()),
                        workflowTestConfiguration(84L, "workflow-of-a-plain-project")));
            when(projectWorkflowService.getWorkflowProjectWorkflows(anyList()))
                .thenReturn(
                    List.of(
                        new ProjectWorkflow(1114L, 4, namedUserWorkflow.getId()),
                        new ProjectWorkflow(1115L, 2, unnamedUserWorkflow.getId()),
                        new ProjectWorkflow(1117L, 3, anonymousUserWorkflow.getId()),
                        new ProjectWorkflow(1116L, 1, "workflow-of-a-plain-project")));
            when(connectedUserProjectService.fetchProjectConnectedUserProject(1114L))
                .thenReturn(Optional.of(new ConnectedUserProject(1050L, 1L, 1114L, 0)));
            when(connectedUserProjectService.fetchProjectConnectedUserProject(1115L))
                .thenReturn(Optional.of(new ConnectedUserProject(1051L, 2L, 1115L, 0)));
            when(connectedUserProjectService.fetchProjectConnectedUserProject(1116L)).thenReturn(Optional.empty());
            when(connectedUserProjectService.fetchProjectConnectedUserProject(1117L))
                .thenReturn(Optional.of(new ConnectedUserProject(1052L, 3L, 1117L, 0)));
            connectedUsers.add(connectedUser(1050L, "Jane Doe", "jane@example.com", "jane-1"));
            connectedUsers.add(connectedUser(1051L, null, "john@example.com", "user-2"));
            connectedUsers.add(connectedUser(1052L, " ", null, "user-3"));

            List<IntegrationConnectionWorkflowDTO> integrationConnectionWorkflowDTOs =
                connectionFacade.getConnectionWorkflows(CONNECTION_ID);

            assertThat(integrationConnectionWorkflowDTOs)
                .extracting(
                    IntegrationConnectionWorkflowDTO::id, IntegrationConnectionWorkflowDTO::parentId,
                    IntegrationConnectionWorkflowDTO::parentName, IntegrationConnectionWorkflowDTO::parentType,
                    IntegrationConnectionWorkflowDTO::parentVersion, IntegrationConnectionWorkflowDTO::usage,
                    IntegrationConnectionWorkflowDTO::workflowName)
                .containsExactlyInAnyOrder(
                    tuple(
                        81L, 1050L, "Jane Doe", IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER, 4,
                        IntegrationConnectionWorkflowDTO.Usage.EDITOR, "Route invoice emails"),
                    tuple(
                        82L, 1050L, "Jane Doe", IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER, 4,
                        IntegrationConnectionWorkflowDTO.Usage.EDITOR, "Route invoice emails"),
                    tuple(
                        83L, 1051L, "john@example.com", IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER, 2,
                        IntegrationConnectionWorkflowDTO.Usage.EDITOR, "Archive receipts"),
                    tuple(
                        85L, 1052L, "user-3", IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER, 3,
                        IntegrationConnectionWorkflowDTO.Usage.EDITOR, "Tag expenses"));
        }

        @Test
        void testGetConnectionWorkflowsRejectsAConnectionThatIsNotEmbedded() {
            when(connectionService.fetchConnection(CONNECTION_ID))
                .thenReturn(Optional.of(connection(CONNECTION_ID, PlatformType.AUTOMATION)));

            assertThatThrownBy(() -> connectionFacade.getConnectionWorkflows(CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void testGetConnectionWorkflowsRequiresTheConnectionViewScope() {
            authenticate(
                new UsernamePasswordAuthenticationToken("user", "n/a",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            when(permissionService.isTenantAdmin()).thenReturn(false);
            when(permissionService.hasResourceScope(CONNECTION_ID, "Connection", "CONNECTION_VIEW"))
                .thenReturn(false);

            assertThatThrownBy(() -> connectionFacade.getConnectionWorkflows(CONNECTION_ID))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testGetConnectionWorkflowsListsDeployedWorkflowsOfConnectedUserProjects() {
            Workflow publishedWorkflow = workflow("Route invoice emails");

            when(projectDeploymentWorkflowService.getConnectionProjectDeploymentWorkflows(CONNECTION_ID))
                .thenReturn(
                    List.of(
                        projectDeploymentWorkflow(91L, publishedWorkflow.getId()),
                        projectDeploymentWorkflow(92L, "workflow-of-a-plain-project")));
            when(projectWorkflowService.getWorkflowProjectWorkflows(anyList()))
                .thenReturn(
                    List.of(
                        new ProjectWorkflow(1114L, 2, publishedWorkflow.getId()),
                        new ProjectWorkflow(1116L, 1, "workflow-of-a-plain-project")));
            when(connectedUserProjectService.fetchProjectConnectedUserProject(1114L))
                .thenReturn(Optional.of(new ConnectedUserProject(1050L, 1L, 1114L, 0)));
            when(connectedUserProjectService.fetchProjectConnectedUserProject(1116L)).thenReturn(Optional.empty());
            connectedUsers.add(connectedUser(1050L, "Jane Doe", "jane@example.com", "jane-1"));

            List<IntegrationConnectionWorkflowDTO> integrationConnectionWorkflowDTOs =
                connectionFacade.getConnectionWorkflows(CONNECTION_ID);

            assertThat(integrationConnectionWorkflowDTOs)
                .extracting(
                    IntegrationConnectionWorkflowDTO::id, IntegrationConnectionWorkflowDTO::parentId,
                    IntegrationConnectionWorkflowDTO::parentName, IntegrationConnectionWorkflowDTO::parentType,
                    IntegrationConnectionWorkflowDTO::parentVersion, IntegrationConnectionWorkflowDTO::usage,
                    IntegrationConnectionWorkflowDTO::workflowName)
                .containsExactly(
                    tuple(
                        91L, 1050L, "Jane Doe", IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER, 2,
                        IntegrationConnectionWorkflowDTO.Usage.DEPLOYMENT, "Route invoice emails"));
        }

        private Workflow addWorkflow(Integration integration, String label) {
            Workflow workflow = new Workflow(
                String.valueOf(UUID.randomUUID()), "{\"label\": \"%s\", \"tasks\": []}".formatted(label),
                Workflow.Format.JSON);

            workflows.add(workflow);

            integrationWorkflowService.addWorkflow(integration.getId(), 1, workflow.getId());

            return workflow;
        }

        private static <T> List<T> filterById(List<T> items, List<?> ids, Function<T, ?> idFunction) {
            return items.stream()
                .filter(item -> ids.contains(idFunction.apply(item)))
                .toList();
        }

        private static ConnectedUser connectedUser(
            long id, @Nullable String name, @Nullable String email, String externalId) {

            return new ConnectedUser(Map.of(), email, true, externalId, id, name, 0);
        }

        private Integration createIntegration(String name) {
            Integration integration = new Integration();

            integration.setComponentName("componentName");
            integration.setName(name);

            return integrationService.create(integration);
        }

        private IntegrationInstanceConfigurationWorkflow createIntegrationInstanceConfigurationWorkflow(
            Integration integration, Workflow workflow, long connectionId) {

            IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

            integrationInstanceConfiguration.setAuthorizationType(AuthorizationType.API_KEY);
            integrationInstanceConfiguration.setConnectionParameters(Map.of("key", "admin-value"));
            integrationInstanceConfiguration.setEnvironment(Environment.STAGING);
            integrationInstanceConfiguration.setIntegrationId(integration.getId());
            integrationInstanceConfiguration.setIntegrationVersion(1);
            integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

            integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
                integrationInstanceConfiguration);

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setConnections(
                List.of(
                    new IntegrationInstanceConfigurationWorkflowConnection(connectionId, "connection", "trigger_1")));
            integrationInstanceConfigurationWorkflow.setEnabled(true);
            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfiguration.getId());
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflow.getId());

            return integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
        }

        private Workflow workflow(String label) {
            Workflow workflow = new Workflow(
                String.valueOf(UUID.randomUUID()), "{\"label\": \"%s\", \"tasks\": []}".formatted(label),
                Workflow.Format.JSON);

            workflows.add(workflow);

            return workflow;
        }

        private static ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, String workflowId) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setId(id);
            projectDeploymentWorkflow.setWorkflowId(workflowId);

            return projectDeploymentWorkflow;
        }

        private static WorkflowTestConfiguration workflowTestConfiguration(long id, String workflowId) {
            WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

            workflowTestConfiguration.setId(id);
            workflowTestConfiguration.setEnvironmentId(1L);
            workflowTestConfiguration.setWorkflowId(workflowId);

            return workflowTestConfiguration;
        }
    }

    @Configuration
    @EnableMethodSecurity
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService,
            ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

            AutomationMethodSecurityExpressionHandler handler = new AutomationMethodSecurityExpressionHandler(
                permissionService, methodSecurityExpressionContributorProvider);

            handler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            return handler;
        }
    }
}

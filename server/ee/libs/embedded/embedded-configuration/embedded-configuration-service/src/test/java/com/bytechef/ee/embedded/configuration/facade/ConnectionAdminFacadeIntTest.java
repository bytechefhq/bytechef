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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
        IntegrationIntTestConfiguration.class, ConnectionAdminFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "bytechef.audit.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectionAdminFacadeIntTest {

    @Autowired
    private ConnectionAdminFacade connectionAdminFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @MockitoBean
    private ConnectedUserConnectionService connectedUserConnectionService;

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

        when(permissionService.isTenantAdmin()).thenReturn(false);

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

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(7L);
        when(connectionService.fetchConnection(7L)).thenReturn(Optional.of(connection(7L, PlatformType.EMBEDDED)));

        assertThat(connectionAdminFacade.createConnection(connectionDTO, true)).isEqualTo(7L);

        connectionAdminFacade.deleteConnection(7L);
        connectionAdminFacade.getConnection(7L);
        connectionAdminFacade.getConnections("slack", 1, 2L, 3L);
        connectionAdminFacade.getSharedConnectionIds();
        connectionAdminFacade.updateConnection(7L, "name", List.of(), true, 0);
        connectionAdminFacade.updateConnectionCredentials(7L, Map.of("token", "secret"), 0);

        verify(connectionFacade).updateCredentials(7L, Map.of("token", "secret"), 0);
        verify(connectionFacade).delete(7L);
        verify(connectionFacade).getConnection(7L);
        verify(connectionFacade).getConnections("slack", 1, List.of(), 3L, 2L, PlatformType.EMBEDDED);
        verify(connectionFacade).update(7L, "name", List.of(), 0);
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

        assertThatThrownBy(() -> connectionAdminFacade.createConnection(connectionDTO, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnection(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnections(null, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getSharedConnectionIds())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.updateConnection(7L, "name", List.of(), null, 0))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectionFacade, connectionService, connectedUserConnectionService);
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

            when(permissionService.isTenantAdmin()).thenReturn(true);
            when(connectionService.fetchConnection(AUTOMATION_CONNECTION_ID))
                .thenReturn(Optional.of(connection(AUTOMATION_CONNECTION_ID, PlatformType.AUTOMATION)));
            when(connectionService.fetchConnection(UNKNOWN_CONNECTION_ID)).thenReturn(Optional.empty());
        }

        @Test
        void testDeleteConnectionRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(AUTOMATION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).delete(anyLong());
            verify(connectedUserConnectionService, never()).deleteByConnectionId(anyLong());

            assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_DELETED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", NoSuchElementException.class.getName())
                .containsEntry("connectionId", String.valueOf(AUTOMATION_CONNECTION_ID));
        }

        @Test
        void testGetConnectionRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionAdminFacade.getConnection(AUTOMATION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testUpdateConnectionRejectsAutomationConnection() {
            assertThatThrownBy(
                () -> connectionAdminFacade.updateConnection(AUTOMATION_CONNECTION_ID, "name", List.of(), true, 0))
                    .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).update(anyLong(), anyString(), any(), anyInt());
            verify(connectedUserConnectionService, never()).updateShared(anyLong(), anyBoolean());
        }

        @Test
        void testUnknownConnectionIsRejectedLikeAnAutomationConnection() {
            assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(UNKNOWN_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Connection id=%s not found".formatted(UNKNOWN_CONNECTION_ID));
            assertThatThrownBy(() -> connectionAdminFacade.getConnection(AUTOMATION_CONNECTION_ID))
                .hasMessage("Connection id=%s not found".formatted(AUTOMATION_CONNECTION_ID));

            verify(connectionFacade, never()).delete(anyLong());
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

            when(permissionService.isTenantAdmin()).thenReturn(true);
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

            List<ConnectionDTO> connectionDTOs = connectionAdminFacade.getConnections(null, null, null, null);

            assertThat(connectionDTOs).extracting(ConnectionDTO::id)
                .containsExactly(ADMIN_CONNECTION_ID);
        }

        @Test
        void testGetConnectionThrowsForIntegrationInstanceConnection() {
            stubEmbedded(INTEGRATION_INSTANCE_CONNECTION_ID);

            assertThatThrownBy(() -> connectionAdminFacade.getConnection(INTEGRATION_INSTANCE_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testGetConnectionsExcludesConnectedUserConnections() {
            stubConnections(
                connectionDTO(ADMIN_CONNECTION_ID, List.of()), connectionDTO(CONNECTED_USER_CONNECTION_ID, List.of()));

            List<ConnectionDTO> connectionDTOs = connectionAdminFacade.getConnections(null, null, null, null);

            assertThat(connectionDTOs).extracting(ConnectionDTO::id)
                .containsExactly(ADMIN_CONNECTION_ID);
        }

        @Test
        void testGetConnectionsKeepsSharedAdminConnections() {
            stubConnections(
                connectionDTO(ADMIN_CONNECTION_ID, List.of()), connectionDTO(SHARED_CONNECTION_ID, List.of()));

            when(connectedUserConnectionService.getSharedConnectionIds()).thenReturn(Set.of(SHARED_CONNECTION_ID));

            List<ConnectionDTO> connectionDTOs = connectionAdminFacade.getConnections(null, null, null, null);

            assertThat(connectionDTOs).extracting(ConnectionDTO::id)
                .containsExactly(ADMIN_CONNECTION_ID, SHARED_CONNECTION_ID);
        }

        @Test
        void testGetConnectionThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(() -> connectionAdminFacade.getConnection(CONNECTED_USER_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testUpdateConnectionThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(
                () -> connectionAdminFacade.updateConnection(
                    CONNECTED_USER_CONNECTION_ID, "name", List.of(), null, 0))
                        .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).update(anyLong(), anyString(), any(), anyInt());
        }

        @Test
        void testDeleteConnectionThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(CONNECTED_USER_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).delete(anyLong());
            verify(connectedUserConnectionService, never()).deleteByConnectionId(anyLong());
        }

        @Test
        void testUpdateConnectionCredentialsDelegatesForAdminConnection() {
            stubEmbedded(ADMIN_CONNECTION_ID);

            Map<String, Object> parameters = Map.of("token", "secret");

            connectionAdminFacade.updateConnectionCredentials(ADMIN_CONNECTION_ID, parameters, 2);

            verify(connectionFacade).updateCredentials(ADMIN_CONNECTION_ID, parameters, 2);
        }

        @Test
        void testUpdateConnectionCredentialsThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(
                () -> connectionAdminFacade.updateConnectionCredentials(
                    CONNECTED_USER_CONNECTION_ID, Map.of("token", "secret"), 0))
                        .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).updateCredentials(anyLong(), any(), anyInt());
        }

        @Test
        void testUpdateConnectionTagsThrowsForConnectedUserConnection() {
            stubEmbedded(CONNECTED_USER_CONNECTION_ID);

            assertThatThrownBy(
                () -> connectionAdminFacade.updateConnectionTags(CONNECTED_USER_CONNECTION_ID, List.of()))
                    .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).update(anyLong(), anyList());
        }

        @Test
        void testUpdateConnectionTagsUpdatesAdminConnection() {
            stubEmbedded(ADMIN_CONNECTION_ID);

            List<Tag> tags = List.of(new Tag(11L, "admin"));

            connectionAdminFacade.updateConnectionTags(ADMIN_CONNECTION_ID, tags);

            verify(connectionFacade).update(ADMIN_CONNECTION_ID, tags);
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

            assertThat(connectionAdminFacade.getConnectionTags())
                .extracting(Tag::getName)
                .containsExactly(alphaTag.getName(), zetaTag.getName());

            verify(connectionFacade, never()).getConnections(any(), any(), anyList(), any(), any(), any());
        }

        private ConnectionDTO connectionDTO(long id, List<Tag> tags) {
            return ConnectionDTO.builder()
                .id(id)
                .tags(tags)
                .build();
        }

        private void stubConnections(ConnectionDTO... connectionDTOs) {
            when(connectionFacade.getConnections(any(), any(), anyList(), any(), any(), any(PlatformType.class)))
                .thenReturn(List.of(connectionDTOs));
        }

        private void stubEmbedded(long id) {
            when(connectionService.fetchConnection(id)).thenReturn(Optional.of(connection(id, PlatformType.EMBEDDED)));
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

            return new AutomationMethodSecurityExpressionHandler(
                permissionService, methodSecurityExpressionContributorProvider);
        }
    }
}

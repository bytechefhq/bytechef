/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.automation.configuration.audit.ConnectionAuditMapper;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.ee.automation.configuration.facade.ConnectionReassignmentFacade.MarkPendingOutcome;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EeAutomationConfigurationIntTestConfiguration.class, ConnectionFacadesAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class ConnectionFacadesAuditIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final String DEPARTING_LOGIN = "departing@example.com";
    private static final long DEPARTING_USER_ID = 7L;
    private static final long OTHER_CONNECTION_ID = 43L;
    private static final long WORKSPACE_ID = 1049L;

    @MockitoBean
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionReassignmentFacade connectionReassignmentFacade;

    @MockitoBean
    private ConnectionService connectionService;

    @Autowired
    private OrganizationConnectionFacade organizationConnectionFacade;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WorkspaceConnectionService workspaceConnectionService;

    @MockitoBean
    private WorkspaceUserService workspaceUserService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        User user = new User();

        user.setId(DEPARTING_USER_ID);

        when(userService.fetchUserByLogin(DEPARTING_LOGIN)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMarkingAConnectionPendingReassignmentRecordsTheConnectionAndItsPreviousOwner() {
        when(connectionService.getConnection(CONNECTION_ID))
            .thenReturn(createConnection(CONNECTION_ID, ConnectionStatus.ACTIVE));
        when(connectionService.updateConnectionStatusIfOwnedBy(
            CONNECTION_ID, DEPARTING_LOGIN, ConnectionStatus.PENDING_REASSIGNMENT))
                .thenReturn(true);

        MarkPendingOutcome markPendingOutcome =
            connectionReassignmentFacade.markSingleConnectionPendingReassignment(CONNECTION_ID, DEPARTING_LOGIN);

        assertThat(markPendingOutcome).isEqualTo(MarkPendingOutcome.UPDATED);
        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_PENDING_REASSIGNMENT))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID))
            .containsEntry("previousOwnerUserId", String.valueOf(DEPARTING_USER_ID));
    }

    @Test
    void testASkippedConnectionIsNotRecorded() {
        when(connectionService.getConnection(CONNECTION_ID))
            .thenReturn(createConnection(CONNECTION_ID, ConnectionStatus.ACTIVE));
        when(connectionService.updateConnectionStatusIfOwnedBy(
            CONNECTION_ID, DEPARTING_LOGIN, ConnectionStatus.PENDING_REASSIGNMENT))
                .thenReturn(false);

        MarkPendingOutcome markPendingOutcome =
            connectionReassignmentFacade.markSingleConnectionPendingReassignment(CONNECTION_ID, DEPARTING_LOGIN);

        assertThat(markPendingOutcome).isEqualTo(MarkPendingOutcome.SKIPPED);
        assertThat(findEvents(ConnectionAuditEvents.CONNECTION_PENDING_REASSIGNMENT)).isEmpty();
    }

    @Test
    void testMarkingAMissingConnectionIsRecordedAsAnError() {
        when(connectionService.getConnection(CONNECTION_ID)).thenThrow(new NoSuchElementException("missing"));

        assertThatThrownBy(
            () -> connectionReassignmentFacade.markSingleConnectionPendingReassignment(CONNECTION_ID, DEPARTING_LOGIN))
                .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_PENDING_REASSIGNMENT))
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    @Test
    void testMarkingEveryConnectionOfAMemberRecordsEachMovedConnection() {
        Connection connection = createConnection(CONNECTION_ID, ConnectionStatus.ACTIVE);
        Connection otherConnection = createConnection(OTHER_CONNECTION_ID, ConnectionStatus.REVOKED);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(
                List.of(
                    new WorkspaceConnection(CONNECTION_ID, WORKSPACE_ID),
                    new WorkspaceConnection(OTHER_CONNECTION_ID, WORKSPACE_ID)));
        when(connectionService.getConnections(List.of(CONNECTION_ID, OTHER_CONNECTION_ID)))
            .thenReturn(List.of(connection, otherConnection));
        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);
        when(connectionService.getConnection(OTHER_CONNECTION_ID)).thenReturn(otherConnection);
        when(connectionService.updateConnectionStatusIfOwnedBy(
            CONNECTION_ID, DEPARTING_LOGIN, ConnectionStatus.PENDING_REASSIGNMENT))
                .thenReturn(true);

        BulkReassignResultDTO bulkReassignResultDTO =
            connectionReassignmentFacade.markConnectionsPendingReassignment(WORKSPACE_ID, DEPARTING_LOGIN);

        assertThat(bulkReassignResultDTO.updated()).isEqualTo(1);
        assertThat(bulkReassignResultDTO.skipped()).isEqualTo(1);
        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_PENDING_REASSIGNMENT))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    @Test
    void testUpdatingAnOrganizationConnectionRecordsTheNewName() {
        Connection connection = createConnection(CONNECTION_ID, ConnectionStatus.ACTIVE);

        connection.setVisibility(ResourceVisibility.ORGANIZATION);

        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);

        organizationConnectionFacade.update(CONNECTION_ID, "Renamed", List.of(), 0);

        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID))
            .containsEntry("name", "Renamed");
    }

    @Test
    void testUpdatingAWorkspaceConnectionAsAnOrganizationConnectionIsRecordedAsAnError() {
        Connection connection = createConnection(CONNECTION_ID, ConnectionStatus.ACTIVE);

        connection.setVisibility(ResourceVisibility.WORKSPACE);

        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);

        assertThatThrownBy(() -> organizationConnectionFacade.update(CONNECTION_ID, "Renamed", List.of(), 0))
            .isInstanceOf(ConfigurationException.class);

        assertThat(findSingleEvent(ConnectionAuditEvents.CONNECTION_UPDATED))
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", ConfigurationException.class.getName())
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    private static Connection createConnection(long connectionId, ConnectionStatus connectionStatus) {
        Connection connection = new Connection();

        connection.setCreatedBy(DEPARTING_LOGIN);
        connection.setId(connectionId);
        connection.setName("Connection " + connectionId);
        connection.setStatus(connectionStatus);

        return connection;
    }

    private List<PersistentAuditEvent> findEvents(String eventType) {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        return pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();
    }

    private Map<String, String> findSingleEvent(String eventType) {
        List<PersistentAuditEvent> persistentAuditEvents = findEvents(eventType);

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");

        return persistentAuditEvent.getData();
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class,
        ConnectionAuditMapper.class
    })
    static class AuditConfiguration {

        @Bean
        ConnectionReassignmentFacade connectionReassignmentFacade(
            ConnectionService connectionService, ObjectProvider<MeterRegistry> meterRegistryProvider,
            ProjectDeploymentWorkflowService projectDeploymentWorkflowService, UserService userService,
            WorkflowService workflowService, WorkspaceConnectionService workspaceConnectionService,
            WorkspaceUserService workspaceUserService) {

            return new ConnectionReassignmentFacadeImpl(
                connectionService, meterRegistryProvider, projectDeploymentWorkflowService, userService,
                workflowService, workspaceConnectionService, workspaceUserService);
        }

        @Bean
        OrganizationConnectionFacade organizationConnectionFacade(
            ConnectionFacade connectionFacade, ConnectionService connectionService) {

            return new OrganizationConnectionFacadeImpl(connectionFacade, connectionService);
        }
    }
}

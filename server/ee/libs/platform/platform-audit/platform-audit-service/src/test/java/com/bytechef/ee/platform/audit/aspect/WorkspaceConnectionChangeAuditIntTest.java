/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.audit.WorkspaceConnectionAuditEvents;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class,
    WorkspaceConnectionChangeAuditIntTest.WorkspaceConnectionFacadeConfiguration.class
})
class WorkspaceConnectionChangeAuditIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final String CONFIDENTIAL_TOKEN = "confidential-access-token";
    private static final long USER_ID = 5L;
    private static final long WORKSPACE_ID = 3L;

    @MockitoBean
    private ConnectionFacade connectionFacade;

    @MockitoBean
    private ConnectionService connectionService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private UserService userService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateRecordsTheConnectionWithoutItsCredentials() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("github")
            .environmentId(1)
            .name("GitHub")
            .parameters(Map.of("access_token", CONFIDENTIAL_TOKEN))
            .build();

        User user = new User();

        user.setId(USER_ID);

        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(new Workspace(WORKSPACE_ID, "Default")));
        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.AUTOMATION)))
            .thenReturn(CONNECTION_ID);
        when(connectionFacade.getConnection(CONNECTION_ID)).thenReturn(connectionDTO);

        workspaceConnectionFacade.create(WORKSPACE_ID, connectionDTO);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_CREATED);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID))
            .containsEntry("name", "GitHub")
            .containsEntry("componentName", "github")
            .containsEntry("environmentId", "1")
            .doesNotContainKey("dataError");
        assertThat(persistentAuditEvent.getData()
            .values()).noneMatch(value -> value.contains(CONFIDENTIAL_TOKEN));
    }

    @Test
    void testUpdateRecordsTheNewName() {
        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(createConnection()));

        workspaceConnectionFacade.update(CONNECTION_ID, "Renamed", List.of(), 0);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID))
            .containsEntry("name", "Renamed");
    }

    @Test
    void testDeleteRecordsTheConnection() {
        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(createConnection()));

        workspaceConnectionFacade.delete(CONNECTION_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    @Test
    void testDeleteOfAConnectionOutsideAutomationIsRecordedAsAnError() {
        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceConnectionFacade.delete(CONNECTION_ID))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    @Test
    void testUpdateConnectionCredentialsRecordsTheConnectionWithoutTheCredentials() {
        Connection connection = createConnection();

        connection.setCreatedBy("admin@localhost.com");
        connection.setStatus(ConnectionStatus.ACTIVE);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);

        workspaceConnectionFacade.updateConnectionCredentials(
            CONNECTION_ID, Map.of("access_token", CONFIDENTIAL_TOKEN), 0);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
        assertThat(persistentAuditEvent.getData()
            .values()).noneMatch(value -> value.contains(CONFIDENTIAL_TOKEN));
    }

    @Test
    void testUpdateConnectionCredentialsOfARevokedConnectionIsRecordedAsAnError() {
        Connection connection = createConnection();

        connection.setCreatedBy("admin@localhost.com");
        connection.setStatus(ConnectionStatus.REVOKED);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);

        assertThatThrownBy(
            () -> workspaceConnectionFacade.updateConnectionCredentials(
                CONNECTION_ID, Map.of("access_token", CONFIDENTIAL_TOKEN), 0))
                    .isInstanceOf(ConfigurationException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", ConfigurationException.class.getName())
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    private static Connection createConnection() {
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setType(PlatformType.AUTOMATION);

        return connection;
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    static class WorkspaceConnectionFacadeConfiguration {

        @Bean
        WorkspaceConnectionFacade workspaceConnectionFacade(
            ConnectionFacade connectionFacade, ConnectionService connectionService, UserService userService,
            WorkspaceFacade workspaceFacade) {

            return new WorkspaceConnectionFacadeImpl(
                connectionFacade, mock(ConnectionLifecycleFacade.class), connectionService,
                mock(ResourceVisibilityResolver.class), mock(ObjectProvider.class), mock(PermissionService.class),
                mock(ProjectDeploymentWorkflowService.class), mock(ProjectService.class), userService,
                mock(WorkflowTestConfigurationService.class), mock(WorkspaceConnectionService.class), workspaceFacade);
        }
    }
}

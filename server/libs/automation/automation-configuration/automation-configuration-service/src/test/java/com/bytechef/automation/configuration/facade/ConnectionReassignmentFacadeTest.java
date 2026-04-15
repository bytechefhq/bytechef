/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade.AffectedWorkflow;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade.ConnectionReassignmentItem;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.audit.ConnectionAuditEvent;
import com.bytechef.platform.connection.audit.ConnectionAuditPublisher;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectionReassignmentFacadeTest {

    private static final long WORKSPACE_ID = 1L;
    private static final String USER_LOGIN = "departing-user@example.com";
    private static final String NEW_OWNER_LOGIN = "new-owner@example.com";

    @Mock
    private ConnectionAuditPublisher connectionAuditPublisher;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private UserService userService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    private ConnectionReassignmentFacadeImpl connectionReassignmentFacade;

    @BeforeEach
    void setUp() {
        ObjectProvider<MeterRegistry> meterRegistryProvider = meterRegistryProvider(new SimpleMeterRegistry());

        connectionReassignmentFacade = new ConnectionReassignmentFacadeImpl(
            connectionAuditPublisher, connectionService, meterRegistryProvider, projectDeploymentWorkflowService,
            userService, workflowService, workspaceConnectionService);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> meterRegistryProvider(MeterRegistry meterRegistry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        return provider;
    }

    @Test
    void testGetUnresolvedConnectionsEmptyWorkspace() {
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

        List<ConnectionReassignmentItem> result =
            connectionReassignmentFacade.getUnresolvedConnections(WORKSPACE_ID, USER_LOGIN);

        assertThat(result).isEmpty();

        verify(connectionService, never()).getConnections(any(List.class));
    }

    @Test
    void testGetUnresolvedConnectionsFiltersOnlyUserConnections() {
        WorkspaceConnection workspaceConnection1 = mock(WorkspaceConnection.class);
        WorkspaceConnection workspaceConnection2 = mock(WorkspaceConnection.class);

        when(workspaceConnection1.getConnectionId()).thenReturn(10L);
        when(workspaceConnection2.getConnectionId()).thenReturn(20L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection1, workspaceConnection2));

        Connection userConnection = mock(Connection.class);
        Connection otherUserConnection = mock(Connection.class);

        when(userConnection.getId()).thenReturn(10L);
        when(userConnection.getName()).thenReturn("User Connection");
        when(userConnection.getCreatedBy()).thenReturn(USER_LOGIN);
        when(userConnection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(userConnection.getEnvironmentId()).thenReturn(1);

        when(otherUserConnection.getCreatedBy()).thenReturn("other-user@example.com");

        when(connectionService.getConnections(List.of(10L, 20L)))
            .thenReturn(List.of(userConnection, otherUserConnection));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(List.of(10L)))
            .thenReturn(List.of());

        List<ConnectionReassignmentItem> result =
            connectionReassignmentFacade.getUnresolvedConnections(WORKSPACE_ID, USER_LOGIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .connectionId()).isEqualTo(10L);
        assertThat(result.get(0)
            .connectionName()).isEqualTo("User Connection");
        assertThat(result.get(0)
            .visibility()).isEqualTo(ConnectionVisibility.PRIVATE);
        assertThat(result.get(0)
            .dependentWorkflowCount()).isZero();
    }

    @Test
    void testGetUnresolvedConnectionsCountsDependentWorkflows() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        Connection connection = mock(Connection.class);

        when(connection.getId()).thenReturn(10L);
        when(connection.getName()).thenReturn("Test Connection");
        when(connection.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);
        when(connection.getEnvironmentId()).thenReturn(1);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(connection));

        ProjectDeploymentWorkflow deploymentWorkflow1 = mock(ProjectDeploymentWorkflow.class);
        ProjectDeploymentWorkflow deploymentWorkflow2 = mock(ProjectDeploymentWorkflow.class);
        ProjectDeploymentWorkflowConnection workflowConnection1 = mock(ProjectDeploymentWorkflowConnection.class);
        ProjectDeploymentWorkflowConnection workflowConnection2 = mock(ProjectDeploymentWorkflowConnection.class);

        when(workflowConnection1.getConnectionId()).thenReturn(10L);
        when(workflowConnection2.getConnectionId()).thenReturn(10L);
        when(deploymentWorkflow1.getConnections()).thenReturn(List.of(workflowConnection1));
        when(deploymentWorkflow2.getConnections()).thenReturn(List.of(workflowConnection2));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(List.of(10L)))
            .thenReturn(List.of(deploymentWorkflow1, deploymentWorkflow2));

        List<ConnectionReassignmentItem> result =
            connectionReassignmentFacade.getUnresolvedConnections(WORKSPACE_ID, USER_LOGIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .dependentWorkflowCount()).isEqualTo(2);
    }

    @Test
    void testMarkConnectionsPendingReassignment() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        Connection connection = mock(Connection.class);

        when(connection.getId()).thenReturn(10L);
        when(connection.getName()).thenReturn("Test");
        when(connection.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connection.getEnvironmentId()).thenReturn(1);
        when(connection.getStatus()).thenReturn(ConnectionStatus.ACTIVE);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(connection));
        when(connectionService.getConnection(10L)).thenReturn(connection);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(List.of(10L)))
            .thenReturn(List.of());

        connectionReassignmentFacade.markConnectionsPendingReassignment(WORKSPACE_ID, USER_LOGIN);

        verify(connectionService).updateConnectionStatus(10L, ConnectionStatus.PENDING_REASSIGNMENT);
    }

    @Test
    void testMarkConnectionsPendingReassignmentSkipsRevokedConnections() {
        // REVOKED is terminal (see ConnectionStatus); the facade must check canTransitionTo before
        // Connection.setStatus and skip at INFO rather than surface an IllegalStateException.
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        Connection revokedConnection = mock(Connection.class);

        when(revokedConnection.getId()).thenReturn(10L);
        when(revokedConnection.getName()).thenReturn("Revoked");
        when(revokedConnection.getCreatedBy()).thenReturn(USER_LOGIN);
        when(revokedConnection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(revokedConnection.getEnvironmentId()).thenReturn(1);
        when(revokedConnection.getStatus()).thenReturn(ConnectionStatus.REVOKED);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(revokedConnection));
        when(connectionService.getConnection(10L)).thenReturn(revokedConnection);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(List.of(10L)))
            .thenReturn(List.of());

        var result = connectionReassignmentFacade.markConnectionsPendingReassignment(WORKSPACE_ID, USER_LOGIN);

        verify(connectionService, never()).updateConnectionStatus(eq(10L), any());
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(0);
    }

    @Test
    void testReassignConnectionResetsStatusFromPendingToActive() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.of(mock(User.class)));

        Connection connection = mock(Connection.class);

        when(connection.getStatus()).thenReturn(ConnectionStatus.PENDING_REASSIGNMENT);
        when(connectionService.getConnection(10L)).thenReturn(connection);
        when(connectionService.updateCreatedBy(10L, NEW_OWNER_LOGIN)).thenReturn(connection);

        connectionReassignmentFacade.reassignConnection(WORKSPACE_ID, 10L, NEW_OWNER_LOGIN);

        verify(connectionService).updateCreatedBy(10L, NEW_OWNER_LOGIN);
        verify(connectionService).updateConnectionStatus(10L, ConnectionStatus.ACTIVE);
    }

    @Test
    void testReassignConnectionDoesNotResetStatusWhenActive() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.of(mock(User.class)));

        Connection connection = mock(Connection.class);

        when(connection.getStatus()).thenReturn(ConnectionStatus.ACTIVE);
        when(connectionService.getConnection(10L)).thenReturn(connection);
        when(connectionService.updateCreatedBy(10L, NEW_OWNER_LOGIN)).thenReturn(connection);

        connectionReassignmentFacade.reassignConnection(WORKSPACE_ID, 10L, NEW_OWNER_LOGIN);

        verify(connectionService).updateCreatedBy(10L, NEW_OWNER_LOGIN);
        verify(connectionService, never()).updateConnectionStatus(anyLong(), eq(ConnectionStatus.ACTIVE));
    }

    @Test
    void testReassignConnectionRefusesRevokedRow() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.of(mock(User.class)));

        Connection connection = mock(Connection.class);

        when(connection.getStatus()).thenReturn(ConnectionStatus.REVOKED);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        assertThatThrownBy(
            () -> connectionReassignmentFacade.reassignConnection(WORKSPACE_ID, 10L, NEW_OWNER_LOGIN))
                .isInstanceOf(ConfigurationException.class);

        verify(connectionService, never()).updateCreatedBy(anyLong(), any());
    }

    @Test
    void testReassignAllConnections() {
        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.of(mock(User.class)));

        WorkspaceConnection workspaceConnection1 = mock(WorkspaceConnection.class);
        WorkspaceConnection workspaceConnection2 = mock(WorkspaceConnection.class);

        when(workspaceConnection1.getConnectionId()).thenReturn(10L);
        when(workspaceConnection2.getConnectionId()).thenReturn(20L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection1, workspaceConnection2));

        Connection connection1 = mock(Connection.class);
        Connection connection2 = mock(Connection.class);

        when(connection1.getId()).thenReturn(10L);
        when(connection1.getName()).thenReturn("Conn1");
        when(connection1.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection1.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connection1.getEnvironmentId()).thenReturn(1);

        when(connection2.getId()).thenReturn(20L);
        when(connection2.getName()).thenReturn("Conn2");
        when(connection2.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection2.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);
        when(connection2.getEnvironmentId()).thenReturn(1);

        when(connectionService.getConnections(anyList()))
            .thenReturn(List.of(connection1, connection2));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(anyList()))
            .thenReturn(List.of());

        Connection fetchedConnection1 = mock(Connection.class);
        Connection fetchedConnection2 = mock(Connection.class);

        when(fetchedConnection1.getStatus()).thenReturn(ConnectionStatus.PENDING_REASSIGNMENT);
        when(fetchedConnection2.getStatus()).thenReturn(ConnectionStatus.ACTIVE);
        when(connectionService.getConnection(10L)).thenReturn(fetchedConnection1);
        when(connectionService.getConnection(20L)).thenReturn(fetchedConnection2);
        when(connectionService.updateCreatedBy(10L, NEW_OWNER_LOGIN)).thenReturn(fetchedConnection1);
        when(connectionService.updateCreatedBy(20L, NEW_OWNER_LOGIN)).thenReturn(fetchedConnection2);

        connectionReassignmentFacade.reassignAllConnections(WORKSPACE_ID, USER_LOGIN, NEW_OWNER_LOGIN);

        verify(connectionService).updateCreatedBy(10L, NEW_OWNER_LOGIN);
        verify(connectionService).updateCreatedBy(20L, NEW_OWNER_LOGIN);

        // Every per-row CONNECTION_REASSIGNED event from the same bulk call must carry the same
        // correlationId so audit consumers can reassemble the batch. Pinning this anchors the
        // umbrella+children pattern documented in ConnectionAuditEvent.CONNECTION_SHARES_REPLACED
        // and extended here to reassignment.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionAuditPublisher, times(2)).publish(
            eq(ConnectionAuditEvent.CONNECTION_REASSIGNED), anyLong(), dataCaptor.capture());

        List<Map<String, Object>> publishedPayloads = dataCaptor.getAllValues();

        assertThat(publishedPayloads).hasSize(2);
        assertThat(publishedPayloads.get(0))
            .containsKey("correlationId")
            .containsEntry("newOwnerLogin", NEW_OWNER_LOGIN);

        String correlationId = (String) publishedPayloads.get(0)
            .get("correlationId");

        assertThat(correlationId).isNotBlank();
        assertThat(publishedPayloads.get(1))
            .containsEntry("correlationId", correlationId)
            .containsEntry("newOwnerLogin", NEW_OWNER_LOGIN);
    }

    @Test
    void testGetAffectedWorkflowsEmptyWorkspace() {
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

        List<AffectedWorkflow> result =
            connectionReassignmentFacade.getAffectedWorkflows(WORKSPACE_ID, USER_LOGIN);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetAffectedWorkflowsNoUserConnections() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        Connection otherUserConnection = mock(Connection.class);

        when(otherUserConnection.getCreatedBy()).thenReturn("other-user@example.com");

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(otherUserConnection));

        List<AffectedWorkflow> result =
            connectionReassignmentFacade.getAffectedWorkflows(WORKSPACE_ID, USER_LOGIN);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetAffectedWorkflowsReturnsWorkflowsWithConnectionIds() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        Connection userConnection = mock(Connection.class);

        when(userConnection.getId()).thenReturn(10L);
        when(userConnection.getCreatedBy()).thenReturn(USER_LOGIN);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(userConnection));

        ProjectDeploymentWorkflow deploymentWorkflow = mock(ProjectDeploymentWorkflow.class);
        ProjectDeploymentWorkflowConnection workflowConnection = mock(ProjectDeploymentWorkflowConnection.class);

        when(workflowConnection.getConnectionId()).thenReturn(10L);
        when(deploymentWorkflow.getWorkflowId()).thenReturn("workflow-1");
        when(deploymentWorkflow.getConnections()).thenReturn(List.of(workflowConnection));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(List.of(10L)))
            .thenReturn(List.of(deploymentWorkflow));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn("workflow-1");
        when(workflow.getLabel()).thenReturn("My Workflow");

        when(workflowService.getWorkflows(List.of("workflow-1"))).thenReturn(List.of(workflow));

        List<AffectedWorkflow> result =
            connectionReassignmentFacade.getAffectedWorkflows(WORKSPACE_ID, USER_LOGIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .workflowId()).isEqualTo("workflow-1");
        assertThat(result.get(0)
            .workflowName()).isEqualTo("My Workflow");
        assertThat(result.get(0)
            .connectionIds()).containsExactly(10L);
    }

    @Test
    void testReassignConnectionWithNonExistentUserThrows() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> connectionReassignmentFacade.reassignConnection(WORKSPACE_ID, 10L, NEW_OWNER_LOGIN))
                .isInstanceOf(ConfigurationException.class);

        verify(connectionService, never()).updateCreatedBy(anyLong(), any());
    }

    @Test
    void testReassignConnectionNotInWorkspaceThrows() {
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

        assertThatThrownBy(
            () -> connectionReassignmentFacade.reassignConnection(WORKSPACE_ID, 999L, NEW_OWNER_LOGIN))
                .isInstanceOf(ConfigurationException.class);

        verify(connectionService, never()).updateCreatedBy(anyLong(), any());
    }

    @Test
    void testMarkConnectionsPendingReassignmentContinuesOnPerRowFailure() {
        WorkspaceConnection workspaceConnection1 = mock(WorkspaceConnection.class);
        WorkspaceConnection workspaceConnection2 = mock(WorkspaceConnection.class);

        when(workspaceConnection1.getConnectionId()).thenReturn(10L);
        when(workspaceConnection2.getConnectionId()).thenReturn(20L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection1, workspaceConnection2));

        Connection connection1 = mock(Connection.class);
        Connection connection2 = mock(Connection.class);

        when(connection1.getId()).thenReturn(10L);
        when(connection1.getName()).thenReturn("Test1");
        when(connection1.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection1.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connection1.getEnvironmentId()).thenReturn(1);
        when(connection1.getStatus()).thenReturn(ConnectionStatus.ACTIVE);

        when(connection2.getId()).thenReturn(20L);
        when(connection2.getName()).thenReturn("Test2");
        when(connection2.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection2.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connection2.getEnvironmentId()).thenReturn(1);
        when(connection2.getStatus()).thenReturn(ConnectionStatus.ACTIVE);

        when(connectionService.getConnections(anyList()))
            .thenReturn(List.of(connection1, connection2));
        when(connectionService.getConnection(10L)).thenReturn(connection1);
        when(connectionService.getConnection(20L)).thenReturn(connection2);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(anyList()))
            .thenReturn(List.of());

        when(connectionService.updateConnectionStatus(10L, ConnectionStatus.PENDING_REASSIGNMENT))
            .thenThrow(new IllegalStateException("Invalid state transition"));
        when(connectionService.updateConnectionStatus(20L, ConnectionStatus.PENDING_REASSIGNMENT))
            .thenReturn(connection2);

        var result = connectionReassignmentFacade.markConnectionsPendingReassignment(WORKSPACE_ID, USER_LOGIN);

        verify(connectionService).updateConnectionStatus(10L, ConnectionStatus.PENDING_REASSIGNMENT);
        verify(connectionService).updateConnectionStatus(20L, ConnectionStatus.PENDING_REASSIGNMENT);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.failures())
            .singleElement()
            .satisfies(failure -> {
                assertThat(failure.connectionId()).isEqualTo("10");
                // Non-ConfigurationException errors are sanitized to "Unexpected error: <SimpleName>"
                // so raw JDBC / SQL detail never leaks to the admin UI. The raw message is still
                // logged server-side for operators. errorCode classifies this as UNEXPECTED.
                assertThat(failure.errorCode()).isEqualTo("UNEXPECTED");
                assertThat(failure.message()).isEqualTo("Unexpected error: IllegalStateException");
            });
    }

    @Test
    void testReassignAllConnectionsIsAtomic() {
        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.of(mock(User.class)));

        WorkspaceConnection workspaceConnection1 = mock(WorkspaceConnection.class);
        WorkspaceConnection workspaceConnection2 = mock(WorkspaceConnection.class);

        when(workspaceConnection1.getConnectionId()).thenReturn(10L);
        when(workspaceConnection2.getConnectionId()).thenReturn(20L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection1, workspaceConnection2));

        Connection connection1 = mock(Connection.class);
        Connection connection2 = mock(Connection.class);

        when(connection1.getId()).thenReturn(10L);
        when(connection1.getName()).thenReturn("Conn1");
        when(connection1.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection1.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connection1.getEnvironmentId()).thenReturn(1);

        when(connection2.getId()).thenReturn(20L);
        when(connection2.getName()).thenReturn("Conn2");
        when(connection2.getCreatedBy()).thenReturn(USER_LOGIN);
        when(connection2.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);
        when(connection2.getEnvironmentId()).thenReturn(1);

        when(connectionService.getConnections(anyList()))
            .thenReturn(List.of(connection1, connection2));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(anyList()))
            .thenReturn(List.of());

        Connection fetched1 = mock(Connection.class);

        when(fetched1.getStatus()).thenReturn(ConnectionStatus.PENDING_REASSIGNMENT);
        when(connectionService.getConnection(10L)).thenReturn(fetched1);

        when(connectionService.updateCreatedBy(10L, NEW_OWNER_LOGIN))
            .thenThrow(new NoSuchElementException("Connection not found"));

        assertThatThrownBy(
            () -> connectionReassignmentFacade.reassignAllConnections(WORKSPACE_ID, USER_LOGIN, NEW_OWNER_LOGIN))
                .isInstanceOf(NoSuchElementException.class);

        verify(connectionService, never()).updateCreatedBy(20L, NEW_OWNER_LOGIN);

        // Audit events are registered as AFTER_COMMIT callbacks in reassignAllConnections and must
        // NOT fire when the outer transaction rolls back. A passing atomicity test that didn't check
        // this would leave the "successfully reassigned" audit trail claiming a transfer for a row
        // whose updateCreatedBy never happened.
        verify(connectionAuditPublisher, never())
            .publish(any(), anyLong(), anyMap());
    }

    /**
     * Pins {@code @Transactional(propagation = REQUIRES_NEW)} on
     * {@link ConnectionReassignmentFacadeImpl#markSingleConnectionPendingReassignment(long)} via reflection. The
     * per-row REQUIRES_NEW boundary is load-bearing for partial-failure semantics: without it, one failing row flips
     * the outer transaction to rollback-only and discards every previously "updated" row at commit, even though the DTO
     * the caller receives claims success. The pure-Mockito tests in this class cannot exercise Spring's transactional
     * proxy, so this reflection check is the regression guard — mirroring the pattern
     * ConnectionGraphQlControllerAuthorizationTest uses for {@code @PreAuthorize}.
     */
    @Test
    void testMarkSingleConnectionPendingReassignmentRequiresNewTransactionBoundary() throws NoSuchMethodException {
        java.lang.reflect.Method method = ConnectionReassignmentFacadeImpl.class.getDeclaredMethod(
            "markSingleConnectionPendingReassignment", long.class);

        org.springframework.transaction.annotation.Transactional transactional = method.getAnnotation(
            org.springframework.transaction.annotation.Transactional.class);

        assertThat(transactional)
            .as(
                "markSingleConnectionPendingReassignment must be @Transactional — without it, per-row failures would "
                    + "not roll back independently of the batch and partial-success would become all-or-nothing at "
                    + "commit time.")
            .isNotNull();

        assertThat(transactional.propagation())
            .as(
                "markSingleConnectionPendingReassignment must use Propagation.REQUIRES_NEW. REQUIRED would share the "
                    + "outer batch transaction, so a per-row throw would flip it to rollback-only and discard every "
                    + "previously updated row when the batch method commits.")
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }
}

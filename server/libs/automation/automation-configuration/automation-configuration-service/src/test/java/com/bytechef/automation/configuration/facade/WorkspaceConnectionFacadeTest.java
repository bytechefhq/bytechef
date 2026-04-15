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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectConnectionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectMembershipAccessor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.audit.AuditConnection;
import com.bytechef.platform.connection.audit.ConnectionAuditEvent;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceConnectionFacadeTest {

    private static final long WORKSPACE_ID = 1L;
    private static final String CURRENT_USER = "admin@example.com";

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private ConnectionLifecycleFacade connectionLifecycleFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectConnectionService projectConnectionService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectMembershipAccessor projectMembershipAccessor;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    @Mock
    private WorkspaceFacade workspaceFacade;

    private WorkspaceConnectionFacadeImpl workspaceConnectionFacade;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        // Default the accessor to "return the input unchanged" (CE semantics) so tests that don't care
        // about project-membership narrowing pass. Wrapped in lenient() because most tests in this file
        // don't invoke getConnections() and would otherwise fail Mockito's strict-stubbing check.
        // Individual tests override when they need EE-style filtering behavior.
        lenient()
            .when(projectMembershipAccessor.filterByMembership(anyString(), anyList()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        workspaceConnectionFacade = new WorkspaceConnectionFacadeImpl(
            connectionFacade, connectionLifecycleFacade, connectionService, emptyProvider, projectConnectionService,
            projectDeploymentWorkflowService, projectMembershipAccessor, projectService, userService,
            workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);
    }

    @Test
    void testGetConnectionsPrivateVisibleOnlyToCreator() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(CURRENT_USER);
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);

            WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

            when(workspaceConnection.getConnectionId()).thenReturn(10L);

            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(workspaceConnection));

            // Use the real ConnectionDTO.builder() rather than mocking getters — a rename (e.g. createdBy ->
            // creator) would leave stubs pointing at nothing and silently break test intent. Builders fail at
            // compile time when a getter is renamed, catching the regression at refactor time.
            ConnectionDTO ownedConnection = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.PRIVATE)
                .createdBy(CURRENT_USER)
                .build();
            ConnectionDTO otherConnection = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.PRIVATE)
                .createdBy("other-user@example.com")
                .build();

            when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
                .thenReturn(List.of(ownedConnection, otherConnection));

            when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID)).thenReturn(List.of());
            when(projectConnectionService.getProjectConnectionsByProjectIds(List.of())).thenReturn(List.of());

            List<ConnectionDTO> result =
                workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

            assertThat(result).containsExactly(ownedConnection);
        }
    }

    @Test
    void testGetConnectionsWorkspaceVisibleToAll() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(CURRENT_USER);
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);

            WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

            when(workspaceConnection.getConnectionId()).thenReturn(10L);

            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(workspaceConnection));

            ConnectionDTO workspaceConn = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.WORKSPACE)
                .build();

            when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
                .thenReturn(List.of(workspaceConn));

            when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID)).thenReturn(List.of());
            when(projectConnectionService.getProjectConnectionsByProjectIds(List.of())).thenReturn(List.of());

            List<ConnectionDTO> result =
                workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

            assertThat(result).containsExactly(workspaceConn);
        }
    }

    @Test
    void testGetConnectionsOrganizationVisibleToAll() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(CURRENT_USER);
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);

            WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

            when(workspaceConnection.getConnectionId()).thenReturn(10L);

            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(workspaceConnection));

            ConnectionDTO orgConn = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.ORGANIZATION)
                .build();

            when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
                .thenReturn(List.of(orgConn));

            when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID)).thenReturn(List.of());
            when(projectConnectionService.getProjectConnectionsByProjectIds(List.of())).thenReturn(List.of());

            List<ConnectionDTO> result =
                workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

            assertThat(result).containsExactly(orgConn);
        }
    }

    @Test
    void testGetConnectionsProjectVisibleOnlyWhenShared() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(CURRENT_USER);
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);

            WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

            when(workspaceConnection.getConnectionId()).thenReturn(10L);

            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(workspaceConnection));

            ConnectionDTO sharedProjectConn = ConnectionDTO.builder()
                .id(10L)
                .visibility(ConnectionVisibility.PROJECT)
                .build();
            ConnectionDTO unsharedProjectConn = ConnectionDTO.builder()
                .id(20L)
                .visibility(ConnectionVisibility.PROJECT)
                .build();

            when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
                .thenReturn(List.of(sharedProjectConn, unsharedProjectConn));

            Project project = mock(Project.class);

            when(project.getId()).thenReturn(100L);

            when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID))
                .thenReturn(List.of(project));

            ProjectConnection projectConnection = mock(ProjectConnection.class);

            when(projectConnection.getConnectionId()).thenReturn(10L);

            when(projectConnectionService.getProjectConnectionsByProjectIds(List.of(100L)))
                .thenReturn(List.of(projectConnection));

            List<ConnectionDTO> result =
                workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

            assertThat(result).containsExactly(sharedProjectConn);
        }
    }

    @Test
    void testGetConnectionsEmptyWorkspaceReturnsEmpty() {
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

        List<ConnectionDTO> result = workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void testDemoteToPrivateDeletesProjectConnectionsAndUpdatesVisibility() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(true);

            stubWorkspaceContainsConnection(10L);

            when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(false);

            workspaceConnectionFacade.demoteToPrivate(WORKSPACE_ID, 10L);

            verify(projectConnectionService).deleteByConnectionId(10L);
            verify(connectionService).updateVisibility(10L, ConnectionVisibility.PRIVATE);
        }
    }

    @Test
    void testDemoteToPrivateAllowsCreatorWhenNotAdmin() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(CURRENT_USER);

            stubWorkspaceContainsConnection(10L);

            when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(false);

            ConnectionDTO dto = ConnectionDTO.builder()
                .createdBy(CURRENT_USER)
                .build();

            when(connectionFacade.getConnection(10L)).thenReturn(dto);

            workspaceConnectionFacade.demoteToPrivate(WORKSPACE_ID, 10L);

            verify(connectionService).updateVisibility(10L, ConnectionVisibility.PRIVATE);
        }
    }

    @Test
    void testDemoteToPrivateBlockedWhenNonAdminAndNotCreator() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(CURRENT_USER);

            ConnectionDTO dto = ConnectionDTO.builder()
                .createdBy("someone-else@example.com")
                .build();

            when(connectionFacade.getConnection(10L)).thenReturn(dto);

            assertThatThrownBy(() -> workspaceConnectionFacade.demoteToPrivate(WORKSPACE_ID, 10L))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("administrator or the connection creator");

            verify(connectionService, never()).updateVisibility(eq(10L), any());
            // Authz is evaluated before workspace/deployment validation, so those paths are never
            // consulted for an unauthorized caller — no info leak via differential error messages.
            verify(workspaceConnectionService, never()).getWorkspaceConnections(anyLong());
            verify(projectDeploymentWorkflowService, never()).isConnectionUsed(anyLong());
        }
    }

    @Test
    void testDemoteToPrivateBlockedWhenConnectionIsUsed() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(true);

            stubWorkspaceContainsConnection(10L);

            when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(true);

            assertThatThrownBy(() -> workspaceConnectionFacade.demoteToPrivate(WORKSPACE_ID, 10L))
                .isInstanceOf(ConfigurationException.class);

            verify(projectConnectionService, never()).deleteByConnectionId(10L);
            verify(connectionService, never()).updateVisibility(eq(10L), any());
        }
    }

    @Test
    void testDemoteToPrivateBlockedWhenConnectionNotInWorkspace() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(true);

            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> workspaceConnectionFacade.demoteToPrivate(WORKSPACE_ID, 10L))
                .isInstanceOf(ConfigurationException.class);

            verify(projectConnectionService, never()).deleteByConnectionId(10L);
        }
    }

    @Test
    void testPromoteToWorkspaceUpdatesVisibilityAndAudits() {
        stubWorkspaceContainsConnection(10L);

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        workspaceConnectionFacade.promoteToWorkspace(WORKSPACE_ID, 10L);

        verify(connectionService).updateVisibility(10L, ConnectionVisibility.WORKSPACE);
    }

    /**
     * Pins the {@code CONNECTION_ALREADY_AT_TARGET_VISIBILITY} error-key contract that
     * {@code promoteAllPrivateToWorkspace} relies on to classify concurrent races as {@code skipped} (not
     * {@code failed}). If the error key or the thrown exception class ever changes, bulk promote will start counting
     * benign races as real failures — this test makes that regression explicit.
     */
    @Test
    void testPromoteToWorkspaceThrowsAlreadyAtTargetWhenConnectionIsWorkspace() {
        stubWorkspaceContainsConnection(10L);

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        assertThatThrownBy(() -> workspaceConnectionFacade.promoteToWorkspace(WORKSPACE_ID, 10L))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(ConnectionErrorType.CONNECTION_ALREADY_AT_TARGET_VISIBILITY.getErrorKey()));

        verify(connectionService, never()).updateVisibility(eq(10L), any());
    }

    @Test
    void testShareConnectionToProject() {
        stubWorkspaceContainsConnection(10L);

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        workspaceConnectionFacade.shareConnectionToProject(WORKSPACE_ID, 10L, 100L);

        verify(projectConnectionService).create(10L, 100L);
        verify(connectionService).updateVisibility(10L, ConnectionVisibility.PROJECT);
    }

    @Test
    void testShareConnectionToProjectAbortsWhenConcurrentlyPromotedToWorkspace() {
        // Closes the TOCTOU window between the up-front visibility check (line ~445 in the facade)
        // and the final updateVisibility(PROJECT). If admin B promotes the row to WORKSPACE between
        // those two steps, a blind updateVisibility(PROJECT) would silently demote the row and
        // contradict admin B's intent. The facade re-reads visibility after the insert and aborts
        // with CONNECTION_ALREADY_AT_TARGET_VISIBILITY so bulk callers can classify the outcome as
        // a benign race rather than a real failure. Transaction rollback unwinds the insert.
        stubWorkspaceContainsConnection(10L);

        Connection initialRead = mock(Connection.class);

        when(initialRead.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);

        Connection reReadAfterShare = mock(Connection.class);

        when(reReadAfterShare.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);

        // First call returns PRIVATE (passes the up-front check); second call (post-share re-read)
        // returns WORKSPACE as if admin B promoted concurrently.
        when(connectionService.getConnection(10L))
            .thenReturn(initialRead)
            .thenReturn(reReadAfterShare);

        assertThatThrownBy(() -> workspaceConnectionFacade.shareConnectionToProject(WORKSPACE_ID, 10L, 100L))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(ConnectionErrorType.CONNECTION_ALREADY_AT_TARGET_VISIBILITY.getErrorKey()));

        verify(connectionService, never()).updateVisibility(eq(10L), any());
    }

    @Test
    void testRevokeConnectionFromProjectDemotesWhenNoRemainingProjects() {
        stubWorkspaceContainsConnection(10L);

        when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(false);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of());

        workspaceConnectionFacade.revokeConnectionFromProject(WORKSPACE_ID, 10L, 100L);

        verify(projectConnectionService).delete(10L, 100L);
        verify(connectionService).updateVisibility(10L, ConnectionVisibility.PRIVATE);
    }

    @Test
    void testRevokeConnectionFromProjectKeepsProjectVisibilityWhenOtherProjectsRemain() {
        stubWorkspaceContainsConnection(10L);

        when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(false);

        ProjectConnection remainingProjectConnection = mock(ProjectConnection.class);

        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(remainingProjectConnection));

        workspaceConnectionFacade.revokeConnectionFromProject(WORKSPACE_ID, 10L, 100L);

        verify(projectConnectionService).delete(10L, 100L);
        verify(connectionService, never()).updateVisibility(eq(10L), any());
    }

    @Test
    void testRevokeConnectionFromProjectBlockedWhenConnectionIsUsed() {
        stubWorkspaceContainsConnection(10L);

        when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceConnectionFacade.revokeConnectionFromProject(WORKSPACE_ID, 10L, 100L))
            .isInstanceOf(ConfigurationException.class);

        verify(projectConnectionService, never()).delete(10L, 100L);
    }

    @Test
    void testCreateIncrementsMetricsCounter() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(true);

            io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

            @SuppressWarnings("unchecked")
            ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(registry);

            WorkspaceConnectionFacadeImpl facadeWithMetrics = new WorkspaceConnectionFacadeImpl(
                connectionFacade, connectionLifecycleFacade, connectionService, provider, projectConnectionService,
                projectDeploymentWorkflowService, projectMembershipAccessor, projectService, userService,
                workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO dto = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.WORKSPACE)
                .build();

            // Metric tag reads the PERSISTED visibility rather than the request body, so the returned
            // DTO must carry what ConnectionFacadeImpl.create would have written.
            ConnectionDTO returned = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.WORKSPACE)
                .build();

            when(connectionFacade.create(any(), any())).thenReturn(42L);
            when(connectionFacade.getConnection(42L)).thenReturn(returned);

            facadeWithMetrics.create(WORKSPACE_ID, dto);

            assertThat(registry.find("bytechef_connection_create")
                .tag("visibility", "WORKSPACE")
                .counter()
                .count()).isEqualTo(1.0);
        }
    }

    @Test
    void testCreateMetricUsesPersistedVisibilityNotRequestedVisibility() {
        // The tag reflects what was actually stored, not what the request body asked for —
        // ConnectionFacadeImpl.create may force PRIVATE on CE/embedded paths.
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(true);

            io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

            @SuppressWarnings("unchecked")
            ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(registry);

            WorkspaceConnectionFacadeImpl facadeWithMetrics = new WorkspaceConnectionFacadeImpl(
                connectionFacade, connectionLifecycleFacade, connectionService, provider, projectConnectionService,
                projectDeploymentWorkflowService, projectMembershipAccessor, projectService, userService,
                workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO requestDto = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.WORKSPACE)
                .build();

            ConnectionDTO persistedDto = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.PRIVATE)
                .build();

            when(connectionFacade.create(any(), any())).thenReturn(42L);
            when(connectionFacade.getConnection(42L)).thenReturn(persistedDto);

            facadeWithMetrics.create(WORKSPACE_ID, requestDto);

            assertThat(registry.find("bytechef_connection_create")
                .tag("visibility", "PRIVATE")
                .counter()
                .count()).isEqualTo(1.0);

            assertThat(registry.find("bytechef_connection_create")
                .tag("visibility", "WORKSPACE")
                .counter()).isNull();
        }
    }

    @Test
    void testCreateIncrementsMetricsCounterForPrivateVisibility() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

            @SuppressWarnings("unchecked")
            ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(registry);

            WorkspaceConnectionFacadeImpl facadeWithMetrics = new WorkspaceConnectionFacadeImpl(
                connectionFacade, connectionLifecycleFacade, connectionService, provider, projectConnectionService,
                projectDeploymentWorkflowService, projectMembershipAccessor, projectService, userService,
                workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO dto = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.PRIVATE)
                .build();

            ConnectionDTO returned = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.PRIVATE)
                .build();

            when(connectionFacade.create(any(), any())).thenReturn(7L);
            when(connectionFacade.getConnection(7L)).thenReturn(returned);

            facadeWithMetrics.create(WORKSPACE_ID, dto);

            assertThat(registry.find("bytechef_connection_create")
                .tag("visibility", "PRIVATE")
                .counter()
                .count()).isEqualTo(1.0);
        }
    }

    @Test
    void testPromoteAllPrivateToWorkspaceTreatsAlreadyAtTargetVisibilityAsBenign() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCurrentUserIsWorkspaceMember(securityUtils);

            WorkspaceConnection wc1 = mock(WorkspaceConnection.class);

            when(wc1.getConnectionId()).thenReturn(10L);
            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of(wc1));

            Connection privateConn = mock(Connection.class);

            when(privateConn.getId()).thenReturn(10L);
            when(privateConn.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(privateConn));

            // Simulate the per-row promote racing with another writer: by the time we look up the
            // connection inside promoteToWorkspace it's already WORKSPACE.
            Connection raced = mock(Connection.class);

            when(raced.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);
            when(connectionService.getConnection(10L)).thenReturn(raced);

            var result = workspaceConnectionFacade.promoteAllPrivateToWorkspace(WORKSPACE_ID);

            assertThat(result.attempted()).isEqualTo(1);
            assertThat(result.promoted()).isEqualTo(0);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.failures()).isEmpty();
        }
    }

    @Test
    void testPromoteAllPrivateToWorkspaceCollectsPartialFailures() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCurrentUserIsWorkspaceMember(securityUtils);

            WorkspaceConnection wc1 = mock(WorkspaceConnection.class);
            WorkspaceConnection wc2 = mock(WorkspaceConnection.class);

            when(wc1.getConnectionId()).thenReturn(10L);
            when(wc2.getConnectionId()).thenReturn(20L);
            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(wc1, wc2));

            Connection ok = mock(Connection.class);
            Connection failing = mock(Connection.class);

            when(ok.getId()).thenReturn(10L);
            when(ok.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(failing.getId()).thenReturn(20L);
            when(failing.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(connectionService.getConnections(anyList()))
                .thenReturn(List.of(ok, failing));
            when(connectionService.getConnection(10L)).thenReturn(ok);
            when(connectionService.getConnection(20L)).thenReturn(failing);

            // Stub both updateVisibility calls symmetrically so Mockito strict-stubbing doesn't flag the
            // (10L, WORKSPACE) invocation as a similar-but-mismatched stubbing of the (20L, WORKSPACE) throw.
            when(connectionService.updateVisibility(10L, ConnectionVisibility.WORKSPACE))
                .thenReturn(ok);
            org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(connectionService)
                .updateVisibility(20L, ConnectionVisibility.WORKSPACE);

            var result = workspaceConnectionFacade.promoteAllPrivateToWorkspace(WORKSPACE_ID);

            assertThat(result.attempted()).isEqualTo(2);
            assertThat(result.promoted()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.failures()).hasSize(1);

            // Pin connectionId, errorCode AND message. A regression that swallowed the exception message
            // (e.g. replacing it with the class name or a generic "Error") would otherwise pass this test
            // because the count-and-id assertions alone do not cover message payload fidelity.
            assertThat(result.failures()
                .get(0)
                .connectionId()).isEqualTo("20");
            assertThat(result.failures()
                .get(0)
                .errorCode()).isEqualTo("UNEXPECTED");
            assertThat(result.failures()
                .get(0)
                .message()).isEqualTo("Unexpected error: RuntimeException");
            verify(connectionService).updateVisibility(10L, ConnectionVisibility.WORKSPACE);
        }
    }

    /**
     * Happy-path single-connection promote from PRIVATE. The state matrix is otherwise covered by
     * testPromoteToWorkspaceUpdatesVisibilityAndAudits (PROJECT→WORKSPACE) and the already-at-target test; this pins
     * the most common path so a regression that breaks only the PRIVATE→WORKSPACE transition cannot slip through.
     */
    @Test
    void testPromoteAllPrivateToWorkspacePromotesSinglePrivateConnection() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCurrentUserIsWorkspaceMember(securityUtils);

            WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

            when(workspaceConnection.getConnectionId()).thenReturn(10L);
            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(workspaceConnection));

            Connection privateConnection = mock(Connection.class);

            when(privateConnection.getId()).thenReturn(10L);
            when(privateConnection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(privateConnection));
            when(connectionService.getConnection(10L)).thenReturn(privateConnection);
            when(connectionService.updateVisibility(10L, ConnectionVisibility.WORKSPACE)).thenReturn(privateConnection);

            var result = workspaceConnectionFacade.promoteAllPrivateToWorkspace(WORKSPACE_ID);

            assertThat(result.attempted()).isEqualTo(1);
            assertThat(result.promoted()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.failures()).isEmpty();
            verify(connectionService).updateVisibility(10L, ConnectionVisibility.WORKSPACE);
        }
    }

    @Test
    void testPromoteAllPrivateToWorkspaceContinuesAfterMidLoopFailure() {
        // Pin the "partial failure surfaces to the caller instead of bailing on the first error" contract from
        // CLAUDE.md. Three PRIVATE connections; the middle one throws. The bulk call must continue past it,
        // promote the third, and return a BulkPromoteResult{promoted=2, failed=1, failures=[middle]} so callers
        // can render "2 promoted, 1 failed (see details)" instead of a short-circuit with ambiguous state.
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCurrentUserIsWorkspaceMember(securityUtils);

            WorkspaceConnection wc1 = mock(WorkspaceConnection.class);
            WorkspaceConnection wc2 = mock(WorkspaceConnection.class);
            WorkspaceConnection wc3 = mock(WorkspaceConnection.class);

            when(wc1.getConnectionId()).thenReturn(10L);
            when(wc2.getConnectionId()).thenReturn(20L);
            when(wc3.getConnectionId()).thenReturn(30L);
            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(wc1, wc2, wc3));

            Connection first = mock(Connection.class);
            Connection middle = mock(Connection.class);
            Connection last = mock(Connection.class);

            when(first.getId()).thenReturn(10L);
            when(first.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(middle.getId()).thenReturn(20L);
            when(middle.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(last.getId()).thenReturn(30L);
            when(last.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(connectionService.getConnections(anyList()))
                .thenReturn(List.of(first, middle, last));
            when(connectionService.getConnection(10L)).thenReturn(first);
            when(connectionService.getConnection(20L)).thenReturn(middle);
            when(connectionService.getConnection(30L)).thenReturn(last);

            when(connectionService.updateVisibility(10L, ConnectionVisibility.WORKSPACE))
                .thenReturn(first);
            org.mockito.Mockito.doThrow(new RuntimeException("middle-failed"))
                .when(connectionService)
                .updateVisibility(20L, ConnectionVisibility.WORKSPACE);
            when(connectionService.updateVisibility(30L, ConnectionVisibility.WORKSPACE))
                .thenReturn(last);

            var result = workspaceConnectionFacade.promoteAllPrivateToWorkspace(WORKSPACE_ID);

            assertThat(result.promoted()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures()
                .get(0)
                .connectionId()).isEqualTo("20");
            // Raw RuntimeException messages are sanitized to the simple class name (see
            // WorkspaceConnectionFacadeImpl#sanitizedFailureMessage) so JDBC/DataAccess messages with
            // bind params or schema detail don't leak into the admin toast. Assert the sanitized shape.
            assertThat(result.failures()
                .get(0)
                .message()).isEqualTo("Unexpected error: RuntimeException");

            // Loop continued past the middle failure — last connection was still promoted.
            verify(connectionService).updateVisibility(10L, ConnectionVisibility.WORKSPACE);
            verify(connectionService).updateVisibility(30L, ConnectionVisibility.WORKSPACE);
        }
    }

    @Test
    void testSetConnectionProjectsNoOpDoesNotWriteAnything() {
        stubWorkspaceContainsConnection(10L);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(100L));

        verify(projectConnectionService, never()).create(eq(10L), any(Long.class));
        verify(projectConnectionService, never()).delete(eq(10L), any(Long.class));
        verify(connectionService, never()).updateVisibility(eq(10L), any());
    }

    @Test
    void testCreateRejectsWorkspaceVisibilityForNonAdmin() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);

            ConnectionDTO dto = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.WORKSPACE)
                .build();

            assertThatThrownBy(() -> workspaceConnectionFacade.create(WORKSPACE_ID, dto))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Only administrators");

            verify(connectionFacade, never()).create(any(), any());
        }
    }

    @Test
    void testCreateRejectsProjectVisibility() {
        ConnectionDTO dto = ConnectionDTO.builder()
            .visibility(ConnectionVisibility.PROJECT)
            .build();

        assertThatThrownBy(() -> workspaceConnectionFacade.create(WORKSPACE_ID, dto))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Only PRIVATE or WORKSPACE");

        verify(connectionFacade, never()).create(any(), any());
    }

    @Test
    void testCreateRejectsOrganizationVisibility() {
        ConnectionDTO dto = ConnectionDTO.builder()
            .visibility(ConnectionVisibility.ORGANIZATION)
            .build();

        assertThatThrownBy(() -> workspaceConnectionFacade.create(WORKSPACE_ID, dto))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Only PRIVATE or WORKSPACE");

        verify(connectionFacade, never()).create(any(), any());
    }

    @Test
    void testPromoteAllPrivateToWorkspacePromotesOnlyPrivate() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCurrentUserIsWorkspaceMember(securityUtils);

            WorkspaceConnection wc1 = mock(WorkspaceConnection.class);
            WorkspaceConnection wc2 = mock(WorkspaceConnection.class);
            WorkspaceConnection wc3 = mock(WorkspaceConnection.class);

            when(wc1.getConnectionId()).thenReturn(10L);
            when(wc2.getConnectionId()).thenReturn(20L);
            when(wc3.getConnectionId()).thenReturn(30L);
            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(wc1, wc2, wc3));

            Connection privateConn = mock(Connection.class);
            Connection workspaceConn = mock(Connection.class);
            Connection projectConn = mock(Connection.class);

            when(privateConn.getId()).thenReturn(10L);
            when(privateConn.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
            when(workspaceConn.getVisibility()).thenReturn(ConnectionVisibility.WORKSPACE);
            when(projectConn.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
            when(connectionService.getConnections(List.of(10L, 20L, 30L)))
                .thenReturn(List.of(privateConn, workspaceConn, projectConn));

            // promoteToWorkspace re-fetches a single connection inside its own validation
            when(connectionService.getConnection(10L)).thenReturn(privateConn);

            var result = workspaceConnectionFacade.promoteAllPrivateToWorkspace(WORKSPACE_ID);

            assertThat(result.promoted()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(0);
            verify(connectionService).updateVisibility(10L, ConnectionVisibility.WORKSPACE);
            verify(connectionService, never()).updateVisibility(eq(20L), any());
            verify(connectionService, never()).updateVisibility(eq(30L), any());
        }
    }

    @Test
    void testSetConnectionProjectsEmptyListRevokesAllAndDemotesToPrivate() {
        stubWorkspaceContainsConnection(10L);

        when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(false);

        ProjectConnection existing1 = mock(ProjectConnection.class);
        ProjectConnection existing2 = mock(ProjectConnection.class);

        when(existing1.getProjectId()).thenReturn(100L);
        when(existing2.getProjectId()).thenReturn(200L);
        when(projectConnectionService.getConnectionProjects(10L))
            .thenReturn(List.of(existing1, existing2))
            .thenReturn(List.of(existing2))
            .thenReturn(List.of());

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of());

        verify(projectConnectionService).delete(10L, 100L);
        verify(projectConnectionService).delete(10L, 200L);
        verify(connectionService).updateVisibility(10L, ConnectionVisibility.PRIVATE);
    }

    @Test
    void testSetConnectionProjectsAddsOnlyNewMembers() {
        stubWorkspaceContainsConnection(10L);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(100L, 200L, 300L));

        verify(projectConnectionService).create(10L, 200L);
        verify(projectConnectionService).create(10L, 300L);
        verify(projectConnectionService, never()).create(10L, 100L);
        verify(projectConnectionService, never()).delete(eq(10L), any(Long.class));
    }

    @Test
    void testSetConnectionProjectsNoOpWhenSetEqualsCurrent() {
        stubWorkspaceContainsConnection(10L);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(100L));

        verify(projectConnectionService, never()).create(eq(10L), any(Long.class));
        verify(projectConnectionService, never()).delete(eq(10L), any(Long.class));
        verify(connectionService, never()).updateVisibility(eq(10L), any());
    }

    @Test
    void testSetConnectionProjectsMixedAddAndRemove() {
        stubWorkspaceContainsConnection(10L);

        ProjectConnection kept = mock(ProjectConnection.class);
        ProjectConnection removed = mock(ProjectConnection.class);

        when(kept.getProjectId()).thenReturn(200L);
        when(removed.getProjectId()).thenReturn(100L);

        when(projectConnectionService.getConnectionProjects(10L))
            .thenReturn(List.of(kept, removed))
            .thenReturn(List.of(kept))
            .thenReturn(List.of(kept));

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(200L, 300L));

        verify(projectConnectionService).delete(10L, 100L);
        verify(projectConnectionService).create(10L, 300L);
        verify(projectConnectionService, never()).delete(10L, 200L);
        verify(projectConnectionService, never()).create(10L, 200L);
    }

    @Test
    void testGetConnectionsDoesNotNpeWhenCurrentUserLoginIsNull() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin)
                .thenReturn(null);
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);

            WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

            when(workspaceConnection.getConnectionId()).thenReturn(10L);
            when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
                .thenReturn(List.of(workspaceConnection));

            ConnectionDTO privateConnection = ConnectionDTO.builder()
                .visibility(ConnectionVisibility.PRIVATE)
                .createdBy("some-user@example.com")
                .build();

            when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
                .thenReturn(List.of(privateConnection));

            when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID)).thenReturn(List.of());
            when(projectConnectionService.getProjectConnectionsByProjectIds(List.of())).thenReturn(List.of());

            List<ConnectionDTO> result =
                workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

            assertThat(result).isEmpty();
        }
    }

    @Test
    void testShareConnectionToProjectIsIdempotentOnDoubleShare() {
        stubWorkspaceContainsConnection(10L);

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PRIVATE);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));

        workspaceConnectionFacade.shareConnectionToProject(WORKSPACE_ID, 10L, 100L);

        verify(projectConnectionService, never()).create(eq(10L), any(Long.class));
        verify(connectionService).updateVisibility(10L, ConnectionVisibility.PROJECT);
    }

    @Test
    void testDeleteRethrowsOriginalExceptionAndDoesNotDeleteConnection() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        when(connectionFacade.getConnection(10L)).thenReturn(connectionDTO);

        ConfigurationException scheduledRefreshFailure = new ConfigurationException(
            "scheduler down", ConnectionErrorType.INVALID_CONNECTION);

        org.mockito.Mockito.doThrow(scheduledRefreshFailure)
            .when(connectionLifecycleFacade)
            .deleteScheduledConnectionRefresh(eq(10L), any());

        assertThatThrownBy(() -> workspaceConnectionFacade.delete(10L))
            .isSameAs(scheduledRefreshFailure);

        verify(workspaceConnectionService, never()).deleteWorkspaceConnection(10L);
        verify(connectionFacade, never()).delete(10L);
    }

    @Test
    void testSetConnectionProjectsAuditAnnotationEstablishesCorrelation() throws Exception {
        // Pin the @AuditConnection annotation shape on setConnectionProjects: ConnectionAuditAspect opens a
        // correlation scope when establishCorrelation=true (see ConnectionAuditAspect#establishCorrelation) so
        // every nested per-row share/revoke event inherits the same correlationId. A refactor that drops that
        // flag would silently break the audit-grouping documented in CLAUDE.md — this reflection check is the
        // regression net. The end-to-end UUID propagation is separately verified by
        // testSetConnectionProjectsEmitsUmbrellaAndPerRowEventsUnderOneCorrelationId, which runs the real aspect.
        Method method = WorkspaceConnectionFacadeImpl.class.getMethod(
            "setConnectionProjects", long.class, long.class, List.class);

        AuditConnection annotation = method.getAnnotation(AuditConnection.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.event()).isEqualTo(ConnectionAuditEvent.CONNECTION_SHARES_REPLACED);
        assertThat(annotation.establishCorrelation())
            .as("setConnectionProjects must set establishCorrelation=true so the aspect emits a correlationId "
                + "that operators can grep across the row-level share/revoke events. CLAUDE.md documents this.")
            .isTrue();
    }

    private void stubWorkspaceContainsConnection(long connectionId) {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(connectionId);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));
    }

    @Test
    void testSetConnectionProjectsRoutesPerRowSharesAndRevokesThroughProxy() {
        // Regression guard for the AOP self-invocation fix: setConnectionProjects must route every
        // per-row share/revoke through the Spring proxy so each call is wrapped by @AuditConnection.
        // We install a spy as the self-ref; a refactor that replaces `self().` with a direct call
        // would make the spy miss the per-row invocations and trip this test.
        stubWorkspaceContainsConnection(10L);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        WorkspaceConnectionFacade selfSpy = org.mockito.Mockito.spy(workspaceConnectionFacade);

        workspaceConnectionFacade.setSelf(selfSpy);

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(200L));

        verify(selfSpy).shareConnectionToProject(WORKSPACE_ID, 10L, 200L);
        verify(selfSpy).revokeSingleProjectShareAuditOnly(10L, 100L);
    }

    @Test
    void testSetConnectionProjectsBlocksEmptyingShareListWhileInUseByDeployment() {
        stubWorkspaceContainsConnection(10L);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));
        when(projectDeploymentWorkflowService.isConnectionUsed(10L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("used by active deployments");

        verify(projectConnectionService, never()).delete(eq(10L), any(Long.class));
        verify(connectionService, never()).updateVisibility(eq(10L), any());
    }

    @Test
    void testSetConnectionProjectsAllowsDiffEditWhenInUseAndNetShareSetNonEmpty() {
        // The deployment-in-use guard blocks clearing the share list, NOT reshaping it. Admins must
        // be able to swap projects on an in-use connection as long as at least one share remains.
        stubWorkspaceContainsConnection(10L);

        ProjectConnection existing = mock(ProjectConnection.class);

        when(existing.getProjectId()).thenReturn(100L);
        when(projectConnectionService.getConnectionProjects(10L)).thenReturn(List.of(existing));
        // isConnectionUsed is never consulted because the net share set stays non-empty.

        Connection connection = mock(Connection.class);

        when(connection.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
        when(connectionService.getConnection(10L)).thenReturn(connection);

        workspaceConnectionFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(200L));

        verify(projectConnectionService).create(10L, 200L);
        verify(projectConnectionService).delete(10L, 100L);
        verify(connectionService, never()).updateVisibility(eq(10L), eq(ConnectionVisibility.PRIVATE));
    }

    /**
     * E2E CE-gating assertion: a {@code WORKSPACE} request flowing through {@link WorkspaceConnectionFacadeImpl} into a
     * <i>real</i> {@link com.bytechef.platform.connection.facade.ConnectionFacadeImpl} configured with
     * {@code edition=CE} must land as {@code PRIVATE} at the persistence boundary.
     *
     * <p>
     * Previous tests only prove the gate in isolation ({@code ConnectionFacadeTest}) or mock the downstream facade
     * ({@code testCreateOnCeEditionIsRejected}). Neither would catch a refactor that routes
     * {@code WorkspaceConnectionFacadeImpl.create} around {@code ConnectionFacadeImpl.create} (e.g. through a new
     * "internal" shortcut) and silently loses CE gating. This test closes that gap by wiring the real chain.
     */
    @Test
    void testCeEditionFullStackForcesPrivateVisibilityOnPersist() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(true);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            com.bytechef.platform.component.service.ConnectionDefinitionService connectionDefinitionService =
                mock(com.bytechef.platform.component.service.ConnectionDefinitionService.class);
            ConnectionService realDepConnectionService = mock(ConnectionService.class);
            com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry =
                mock(com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry.class);
            com.bytechef.platform.oauth2.service.OAuth2Service oAuth2Service =
                mock(com.bytechef.platform.oauth2.service.OAuth2Service.class);
            com.bytechef.platform.tag.service.TagService tagService =
                mock(com.bytechef.platform.tag.service.TagService.class);
            WorkflowTestConfigurationService testConfigService = mock(WorkflowTestConfigurationService.class);

            @SuppressWarnings("unchecked")
            ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyProvider = mock(ObjectProvider.class);

            when(emptyProvider.getIfAvailable()).thenReturn(null);

            com.bytechef.platform.connection.facade.ConnectionFacadeImpl realConnectionFacade =
                new com.bytechef.platform.connection.facade.ConnectionFacadeImpl(
                    connectionDefinitionService, realDepConnectionService, "CE", jobPrincipalAccessorRegistry,
                    oAuth2Service, tagService, testConfigService, emptyProvider);

            // Capture the Connection passed to the persistence layer — that's the moment of truth.
            // The real ConnectionFacadeImpl.create call path writes connection.setVisibility(PRIVATE)
            // BEFORE connectionService.create(connection), so the captured instance reflects the final
            // state. After this short-circuit throw the workspace facade's subsequent getConnection()
            // never runs, which keeps the test from having to stub the full toConnectionDTO chain
            // (ConnectionDefinition lookup, JobPrincipalAccessorRegistry, TagService, etc.).
            final java.util.concurrent.atomic.AtomicReference<Connection> persistedRef =
                new java.util.concurrent.atomic.AtomicReference<>();

            when(realDepConnectionService.create(any(Connection.class)))
                .thenAnswer(invocation -> {
                    persistedRef.set(invocation.getArgument(0));

                    throw new CeGateObservedException();
                });

            WorkspaceConnectionFacadeImpl workspaceFacadeWithRealChain = new WorkspaceConnectionFacadeImpl(
                realConnectionFacade, connectionLifecycleFacade, connectionService, emptyProvider,
                projectConnectionService, projectDeploymentWorkflowService, projectMembershipAccessor, projectService,
                userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            ConnectionDTO requestDto = ConnectionDTO.builder()
                .componentName("dummy")
                .name("my-conn")
                .visibility(ConnectionVisibility.WORKSPACE)
                .build();

            assertThatThrownBy(() -> workspaceFacadeWithRealChain.create(WORKSPACE_ID, requestDto))
                .isInstanceOf(CeGateObservedException.class);

            Connection persisted = persistedRef.get();

            assertThat(persisted)
                .as("connectionService.create must have been invoked through the real CE chain")
                .isNotNull();

            assertThat(persisted.getVisibility())
                .as("CE forces PRIVATE regardless of the request body; dropping the gate would silently expose "
                    + "workspace-wide connections on CE installs.")
                .isEqualTo(ConnectionVisibility.PRIVATE);
        }
    }

    /** Short-circuit sentinel thrown at the persistence boundary to isolate the CE-gate assertion. */
    private static final class CeGateObservedException extends RuntimeException {
    }

    /**
     * E2E correlation-ID assertion: {@code setConnectionProjects} is annotated with
     * {@code @AuditConnection(event=CONNECTION_SHARES_REPLACED, establishCorrelation=true)}, and the per-row
     * {@code shareConnectionToProject} / {@code revokeSingleProjectShareAuditOnly} methods are separately audited. The
     * contract is that all emitted events share one correlation ID so audit consumers can group umbrella + children
     * into a single logical operation.
     *
     * <p>
     * The existing {@code ConnectionAuditAspectTest} proves the aspect machinery propagates correlation IDs using a
     * hand-built {@code JoinPoint}. What that test does NOT prove: that the {@code @AuditConnection} annotations are
     * actually present on the right real methods of {@code WorkspaceConnectionFacadeImpl}. A refactor that removed
     * {@code establishCorrelation=true} from {@code setConnectionProjects} — or moved the share/revoke work into an
     * un-annotated helper — would leave the aspect-unit test green while the production correlation chain silently
     * broke.
     *
     * <p>
     * This test wires the real {@link com.bytechef.platform.connection.audit.ConnectionAuditAspect} around a Spring-AOP
     * proxy of the real facade, captures every {@code ConnectionAuditPublisher.publish()} call, and asserts the
     * correlation IDs actually match across the umbrella + the per-row events.
     */
    @Test
    void testSetConnectionProjectsEmitsUmbrellaAndPerRowEventsUnderOneCorrelationId() throws Exception {
        // setConnectionProjects and its nested share/revoke calls do not touch SecurityUtils, so no MockedStatic
        // guard is needed here. The aspect under test (ConnectionAuditAspect) also avoids SecurityUtils.
        stubWorkspaceContainsConnection(10L);

        // Diff: currently shared with {100, 101}; requested {100, 200}. Expected: add 200, revoke 101.
        ProjectConnection existing1 = mock(ProjectConnection.class);
        ProjectConnection existing2 = mock(ProjectConnection.class);

        when(existing1.getProjectId()).thenReturn(100L);
        when(existing2.getProjectId()).thenReturn(101L);
        when(projectConnectionService.getConnectionProjects(10L))
            .thenReturn(List.of(existing1, existing2));

        // shareConnectionToProject's idempotence check re-reads the project list; return only the
        // pre-existing entries so the "already shared" short-circuit does not fire for projectId=200.
        // (Called once per shareConnectionToProject invocation via the proxy.)
        Connection persisted = mock(Connection.class);

        when(persisted.getVisibility()).thenReturn(ConnectionVisibility.PROJECT);
        lenient()
            .when(connectionService.getConnection(10L))
            .thenReturn(persisted);

        com.bytechef.platform.connection.audit.ConnectionAuditPublisher publisher =
            mock(com.bytechef.platform.connection.audit.ConnectionAuditPublisher.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        // Real aspect — same class wired in production. Uses a minimal StaticApplicationContext so
        // @beanName.method() SpEL lookups resolve (none are used by shares_replaced but the aspect
        // constructor requires a context).
        org.springframework.context.support.StaticApplicationContext applicationContext =
            new org.springframework.context.support.StaticApplicationContext();

        applicationContext.refresh();

        com.bytechef.platform.connection.audit.ConnectionAuditAspect auditAspect =
            new com.bytechef.platform.connection.audit.ConnectionAuditAspect(
                applicationContext, publisher, emptyProvider);

        // Wrap the facade in a Spring-AOP proxy that routes @AuditConnection invocations through the real
        // aspect. Using the real aspect (not a mocked one) is load-bearing — the correlation push/pop
        // logic inside establishCorrelation is exactly what this test is pinning.
        org.springframework.aop.aspectj.annotation.AspectJProxyFactory proxyFactory =
            new org.springframework.aop.aspectj.annotation.AspectJProxyFactory(workspaceConnectionFacade);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(auditAspect);

        WorkspaceConnectionFacadeImpl proxiedFacade = proxyFactory.getProxy();

        // Self-reference on the impl must route through the proxy so self-invocations (the per-row
        // share/revoke calls inside setConnectionProjects) are intercepted by the aspect too.
        workspaceConnectionFacade.setSelf(proxiedFacade);

        proxiedFacade.setConnectionProjects(WORKSPACE_ID, 10L, List.of(100L, 200L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> dataCaptor =
            ArgumentCaptor.forClass(java.util.Map.class);

        verify(publisher, org.mockito.Mockito.atLeast(3))
            .publish(any(ConnectionAuditEvent.class), anyLong(),
                dataCaptor.capture());

        List<java.util.Map<String, Object>> capturedData = dataCaptor.getAllValues();

        // At least three events should fire: CONNECTION_SHARES_REPLACED (umbrella) +
        // CONNECTION_SHARED (200) + CONNECTION_REVOKED (101). All must share one correlation ID.
        assertThat(capturedData)
            .as("umbrella + per-row events must all have been published")
            .hasSizeGreaterThanOrEqualTo(3);

        java.util.Set<Object> distinctCorrelationIds = capturedData.stream()
            .map(data -> data.get("correlationId"))
            .collect(java.util.stream.Collectors.toSet());

        assertThat(distinctCorrelationIds)
            .as("umbrella and per-row events must share exactly one correlation ID; a regression that "
                + "drops establishCorrelation=true or routes share/revoke through an un-annotated helper "
                + "would show multiple IDs (or null) here.")
            .hasSize(1);

        assertThat(distinctCorrelationIds.iterator()
            .next())
                .as("correlation ID must be non-null and parseable as UUID")
                .isInstanceOf(String.class)
                .satisfies(id -> UUID.fromString((String) id));
    }

    private void stubCurrentUserIsWorkspaceMember(MockedStatic<SecurityUtils> securityUtils) {
        securityUtils.when(SecurityUtils::getCurrentUserLogin)
            .thenReturn(CURRENT_USER);

        User currentUser = mock(User.class);

        when(currentUser.getId()).thenReturn(99L);
        when(userService.fetchUserByLogin(CURRENT_USER)).thenReturn(java.util.Optional.of(currentUser));

        Workspace workspace = mock(Workspace.class);

        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(workspaceFacade.getUserWorkspaces(99L)).thenReturn(List.of(workspace));
    }
}

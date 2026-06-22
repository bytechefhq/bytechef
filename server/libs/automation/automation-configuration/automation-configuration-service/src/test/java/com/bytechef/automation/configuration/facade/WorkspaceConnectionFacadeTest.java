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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.event.ConnectionCreatedEvent;
import com.bytechef.platform.connection.event.ConnectionDeletedEvent;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceConnectionFacadeTest {

    private static final long WORKSPACE_ID = 1L;
    private static final String CURRENT_USER = "admin@example.com";

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private ConnectionLifecycleFacade connectionLifecycleFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ResourceVisibilityResolver resourceVisibilityResolver;

    @Mock
    private ProjectService projectService;

    @Mock
    private TagService tagService;

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

        workspaceConnectionFacade = new WorkspaceConnectionFacadeImpl(
            applicationEventPublisher, connectionFacade, connectionLifecycleFacade, connectionService,
            resourceVisibilityResolver, emptyProvider, projectDeploymentWorkflowService, projectService, tagService,
            userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);
    }

    @Test
    void testGetConnectionsDelegatesToVisibilityResolver() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        ConnectionDTO connection = ConnectionDTO.builder()
            .id(10L)
            .visibility(ResourceVisibility.PRIVATE)
            .createdBy(CURRENT_USER)
            .build();
        List<ConnectionDTO> allConnections = List.of(connection);

        when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
            .thenReturn(allConnections);
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(WORKSPACE_ID), any()))
            .thenReturn(Set.of(10L));
        lenient().when(connectionFacade.getAiProviderConnections(null, null, null, null))
            .thenReturn(List.of());

        List<ConnectionDTO> result = workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

        assertThat(result).isEqualTo(allConnections);
    }

    @Test
    void testGetConnectionsEmptyWorkspaceReturnsEmpty() {
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());
        when(connectionFacade.getAiProviderConnections(null, null, null, null)).thenReturn(List.of());

        List<ConnectionDTO> result = workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetConnectionsIncludesAiProviderConnectionsWhenWorkspaceEmpty() {
        // C1 regression guard: projected connections must surface even when the workspace has no
        // real connections (i.e., the workspace_connection join table is empty).
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenReturn(List.of());

        ConnectionDTO aiProviderConnection = ConnectionDTO.builder()
            .id(-1L)
            .componentName("openAi")
            .build();

        when(connectionFacade.getAiProviderConnections(null, null, null, null))
            .thenReturn(List.of(aiProviderConnection));

        List<ConnectionDTO> result = workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .componentName()).isEqualTo("openAi");
    }

    @Test
    void testGetConnectionsAppendsAiProviderConnectionsToWorkspaceConnections() {
        // Both a real workspace connection AND an AI provider connection should appear in the result.
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(10L);

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        ConnectionDTO realConnection = ConnectionDTO.builder()
            .id(10L)
            .componentName("slack")
            .visibility(ResourceVisibility.PRIVATE)
            .build();
        List<ConnectionDTO> workspaceConnections = List.of(realConnection);

        when(connectionFacade.getConnections(null, null, List.of(10L), null, null, PlatformType.AUTOMATION))
            .thenReturn(workspaceConnections);
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(WORKSPACE_ID), any()))
            .thenReturn(Set.of(10L));

        ConnectionDTO aiProviderConnection = ConnectionDTO.builder()
            .id(-1L)
            .componentName("openAi")
            .build();

        when(connectionFacade.getAiProviderConnections(null, null, null, null))
            .thenReturn(List.of(aiProviderConnection));

        List<ConnectionDTO> result = workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ConnectionDTO::componentName)
            .containsExactlyInAnyOrder("slack", "openAi");
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
                applicationEventPublisher, connectionFacade, connectionLifecycleFacade, connectionService,
                resourceVisibilityResolver, provider, projectDeploymentWorkflowService, projectService, tagService,
                userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO dto = ConnectionDTO.builder()
                .visibility(ResourceVisibility.WORKSPACE)
                .build();

            // Metric tag reads the PERSISTED visibility rather than the request body, so the returned
            // DTO must carry what ConnectionFacadeImpl.create would have written.
            ConnectionDTO returned = ConnectionDTO.builder()
                .visibility(ResourceVisibility.WORKSPACE)
                .build();

            when(connectionFacade.create(any(), any())).thenReturn(42L);
            when(connectionFacade.getConnection(42L)).thenReturn(returned);

            facadeWithMetrics.create(WORKSPACE_ID, dto);

            assertThat(registry.find("bytechef_connection_create")
                .tag("visibility", "WORKSPACE")
                .counter()
                .count()).isEqualTo(1.0);

            // CE no longer audits directly — it publishes a plain domain event that an EE listener audits.
            // The event carries the PERSISTED visibility (WORKSPACE here), matching the metric tag.
            verify(applicationEventPublisher)
                .publishEvent(new ConnectionCreatedEvent(42L, ResourceVisibility.WORKSPACE));
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
                applicationEventPublisher, connectionFacade, connectionLifecycleFacade, connectionService,
                resourceVisibilityResolver, provider, projectDeploymentWorkflowService, projectService, tagService,
                userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO requestDto = ConnectionDTO.builder()
                .visibility(ResourceVisibility.WORKSPACE)
                .build();

            ConnectionDTO persistedDto = ConnectionDTO.builder()
                .visibility(ResourceVisibility.PRIVATE)
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
                applicationEventPublisher, connectionFacade, connectionLifecycleFacade, connectionService,
                resourceVisibilityResolver, provider, projectDeploymentWorkflowService, projectService, tagService,
                userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO dto = ConnectionDTO.builder()
                .visibility(ResourceVisibility.PRIVATE)
                .build();

            ConnectionDTO returned = ConnectionDTO.builder()
                .visibility(ResourceVisibility.PRIVATE)
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
    void testCreateAllowsWorkspaceVisibilityForNonAdmin() {
        // WORKSPACE is the default every connection is created with, so gating it behind ROLE_ADMIN would fail
        // every ordinary create. The gate made sense only while WORKSPACE was a promotion out of a private
        // default. This test is the inverse of the one it replaces, and exists so a future reviewer sees the
        // removal was deliberate rather than an oversight.
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN"))
                .thenReturn(false);
            stubCurrentUserIsWorkspaceMember(securityUtils);

            ConnectionDTO dto = ConnectionDTO.builder()
                .visibility(ResourceVisibility.WORKSPACE)
                .build();

            when(connectionFacade.create(dto, PlatformType.AUTOMATION)).thenReturn(10L);
            when(connectionFacade.getConnection(10L)).thenReturn(dto);

            workspaceConnectionFacade.create(WORKSPACE_ID, dto);

            verify(connectionFacade).create(dto, PlatformType.AUTOMATION);
        }
    }

    @Test
    void testCreateRejectsOrganizationVisibility() {
        ConnectionDTO dto = ConnectionDTO.builder()
            .visibility(ResourceVisibility.ORGANIZATION)
            .build();

        assertThatThrownBy(() -> workspaceConnectionFacade.create(WORKSPACE_ID, dto))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Only PRIVATE or WORKSPACE");

        verify(connectionFacade, never()).create(any(), any());
    }

    @Test
    void testDeleteRethrowsOriginalExceptionAndDoesNotDeleteConnection() {
        ConfigurationException scheduledRefreshFailure = new ConfigurationException(
            "scheduler down", ConnectionErrorType.INVALID_CONNECTION);

        org.mockito.Mockito.doThrow(scheduledRefreshFailure)
            .when(connectionLifecycleFacade)
            .deleteScheduledConnectionRefresh(eq(10L), any());

        assertThatThrownBy(() -> workspaceConnectionFacade.delete(10L))
            .isSameAs(scheduledRefreshFailure);

        verify(workspaceConnectionService, never()).deleteWorkspaceConnection(10L);
        verify(connectionFacade, never()).delete(10L);
        verify(applicationEventPublisher, never()).publishEvent(any(ConnectionDeletedEvent.class));
    }

    @Test
    void testDeleteCancelsScheduledRefreshBeforeDeletingConnection() {
        // Pin the call ordering: scheduled-refresh cancellation MUST happen BEFORE the workspace-connection /
        // connection-facade deletes. A refactor that reorders these would leave a scheduler firing for a
        // deleted connection id indefinitely (the failure path is already pinned by
        // testDeleteRethrowsOriginalExceptionAndDoesNotDeleteConnection; this test pins the success path).
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
            connectionLifecycleFacade, workspaceConnectionService, connectionFacade);

        workspaceConnectionFacade.delete(10L);

        inOrder.verify(connectionLifecycleFacade)
            .deleteScheduledConnectionRefresh(eq(10L), any());
        inOrder.verify(workspaceConnectionService)
            .deleteWorkspaceConnection(10L);
        inOrder.verify(connectionFacade)
            .delete(10L);

        // CE no longer audits directly — it publishes a plain domain event that an EE listener audits.
        verify(applicationEventPublisher).publishEvent(new ConnectionDeletedEvent(10L));
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
    void testCeEditionFullStackForcesWorkspaceVisibilityOnPersist() {
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
            TagService localTagService = mock(TagService.class);
            WorkflowTestConfigurationService testConfigService = mock(WorkflowTestConfigurationService.class);

            @SuppressWarnings("unchecked")
            ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyProvider = mock(ObjectProvider.class);

            when(emptyProvider.getIfAvailable()).thenReturn(null);

            com.bytechef.platform.connection.facade.ConnectionFacadeImpl realConnectionFacade =
                new com.bytechef.platform.connection.facade.ConnectionFacadeImpl(
                    connectionDefinitionService, realDepConnectionService, "CE", jobPrincipalAccessorRegistry,
                    oAuth2Service, localTagService, testConfigService, emptyProvider);

            // Capture the Connection passed to the persistence layer — that's the moment of truth.
            // The real ConnectionFacadeImpl.create call path writes connection.setVisibility(WORKSPACE)
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
                applicationEventPublisher, realConnectionFacade, connectionLifecycleFacade, connectionService,
                resourceVisibilityResolver, emptyProvider, projectDeploymentWorkflowService, projectService,
                tagService, userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

            ConnectionDTO requestDto = ConnectionDTO.builder()
                .componentName("dummy")
                .name("my-conn")
                .visibility(ResourceVisibility.WORKSPACE)
                .build();

            assertThatThrownBy(() -> workspaceFacadeWithRealChain.create(WORKSPACE_ID, requestDto))
                .isInstanceOf(CeGateObservedException.class);

            Connection persisted = persistedRef.get();

            assertThat(persisted)
                .as("connectionService.create must have been invoked through the real CE chain")
                .isNotNull();

            assertThat(persisted.getVisibility())
                .as("CE forces WORKSPACE regardless of the request body; CE has no visibility picker and no "
                    + "authorization boundary between workspace members, so a PRIVATE request would produce a "
                    + "connection its own workspace could not see and no UI could ever share.")
                .isEqualTo(ResourceVisibility.WORKSPACE);
        }
    }

    /** Short-circuit sentinel thrown at the persistence boundary to isolate the CE-gate assertion. */
    private static final class CeGateObservedException extends RuntimeException {
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

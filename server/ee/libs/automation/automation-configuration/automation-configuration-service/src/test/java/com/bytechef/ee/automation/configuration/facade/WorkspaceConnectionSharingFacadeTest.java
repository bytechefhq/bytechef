/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Behavioural tests for connection visibility and sharing. The authorization gates themselves are enforced through the
 * method-security proxy in {@link WorkspaceConnectionFacadeIntTest}; these exercise the validation that runs after
 * them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceConnectionSharingFacadeTest {

    private static final long CONNECTION_ID = 10L;
    private static final long GRANTEE_ID = 8L;
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ResourceGrantService resourceGrantService;

    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    @Mock
    private WorkspaceUserService workspaceUserService;

    private WorkspaceConnectionSharingFacadeImpl workspaceConnectionSharingFacade;

    @BeforeEach
    void setUp() {
        WorkspaceConnection workspaceConnection = mock(WorkspaceConnection.class);

        when(workspaceConnection.getConnectionId()).thenReturn(CONNECTION_ID);
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(workspaceConnection));

        when(workspaceUserService.isWorkspaceMember(GRANTEE_ID, WORKSPACE_ID)).thenReturn(true);

        workspaceConnectionSharingFacade = new WorkspaceConnectionSharingFacadeImpl(
            connectionService, projectDeploymentWorkflowService, resourceGrantService, connectionPolicyRegistry(),
            workspaceConnectionService, workspaceUserService);
    }

    @Test
    void testSetVisibilityToPrivateBlockedWhenConnectionIsUsedByDeployment() {
        when(projectDeploymentWorkflowService.isConnectionUsed(CONNECTION_ID)).thenReturn(true);

        assertThatThrownBy(
            () -> workspaceConnectionSharingFacade.setConnectionVisibility(
                WORKSPACE_ID, CONNECTION_ID, ResourceVisibility.PRIVATE))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("used by a deployment");

        verify(connectionService, never()).updateVisibility(anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testSetVisibilityToPrivateRevokesGrantsRetainedFromWorkspaceVisibility() {
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setVisibility(ResourceVisibility.WORKSPACE);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));

        workspaceConnectionSharingFacade.setConnectionVisibility(WORKSPACE_ID, CONNECTION_ID,
            ResourceVisibility.PRIVATE);

        verify(resourceGrantService).deleteGrants("Connection", CONNECTION_ID);
        verify(connectionService).updateVisibility(CONNECTION_ID, ResourceVisibility.PRIVATE);
    }

    @Test
    void testSetVisibilityToPrivateOnAPrivateConnectionKeepsItsGrants() {
        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setVisibility(ResourceVisibility.PRIVATE);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));

        workspaceConnectionSharingFacade.setConnectionVisibility(WORKSPACE_ID, CONNECTION_ID,
            ResourceVisibility.PRIVATE);

        verify(resourceGrantService, never()).deleteGrants(anyString(), anyLong());
    }

    @Test
    void testWideningVisibilityDoesNotCheckDeploymentUsage() {
        workspaceConnectionSharingFacade.setConnectionVisibility(WORKSPACE_ID, CONNECTION_ID,
            ResourceVisibility.WORKSPACE);

        // Widening can never strand a deployment, so the usage query — a real database round-trip — must not run.
        verify(projectDeploymentWorkflowService, never()).isConnectionUsed(anyLong());
        verify(connectionService).updateVisibility(CONNECTION_ID, ResourceVisibility.WORKSPACE);
    }

    @Test
    void testSetVisibilityBlockedWhenConnectionNotInWorkspace() {
        assertThatThrownBy(
            () -> workspaceConnectionSharingFacade.setConnectionVisibility(
                WORKSPACE_ID, 999L, ResourceVisibility.WORKSPACE))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("does not belong to workspace");
    }

    @Test
    void testSetVisibilityRejectsOrganization() {
        assertThatThrownBy(
            () -> workspaceConnectionSharingFacade.setConnectionVisibility(
                WORKSPACE_ID, CONNECTION_ID, ResourceVisibility.ORGANIZATION))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("organization connection facade");
    }

    @Test
    void testGrantRejectsUserOutsideOwningWorkspace() {
        when(workspaceUserService.isWorkspaceMember(99L, WORKSPACE_ID)).thenReturn(false);

        assertThatThrownBy(
            () -> workspaceConnectionSharingFacade.grantConnectionAccess(WORKSPACE_ID, CONNECTION_ID, 99L))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(
                    exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        // Same error key as an unknown connection, so a grantor cannot enumerate user ids by
                        // comparing responses.
                        .isEqualTo(ConnectionErrorType.INVALID_CONNECTION.getErrorKey()));

        verify(resourceGrantService, never()).grant(anyString(), anyLong(), anyLong());
    }

    @Test
    void testGrantDelegatesToTheGrantService() {
        workspaceConnectionSharingFacade.grantConnectionAccess(WORKSPACE_ID, CONNECTION_ID, GRANTEE_ID);

        verify(resourceGrantService).grant("Connection", CONNECTION_ID, GRANTEE_ID);
    }

    @Test
    void testRevokeDoesNotRequireMembership() {
        when(workspaceUserService.isWorkspaceMember(GRANTEE_ID, WORKSPACE_ID)).thenReturn(false);

        // Someone removed from the workspace must still be revocable, otherwise their grant would be stranded.
        workspaceConnectionSharingFacade.revokeConnectionAccess(WORKSPACE_ID, CONNECTION_ID, GRANTEE_ID);

        verify(resourceGrantService).revoke("Connection", CONNECTION_ID, GRANTEE_ID);
    }

    private static ResourceVisibilityPolicyRegistry connectionPolicyRegistry() {
        return new ResourceVisibilityPolicyRegistry(
            List.of(
                new ResourceVisibilityPolicy() {

                    @Override
                    public String resourceType() {
                        return "Connection";
                    }

                    @Override
                    public ResourceVisibility defaultVisibility() {
                        return ResourceVisibility.WORKSPACE;
                    }

                    @Override
                    public Set<ResourceVisibility> supportedVisibilities() {
                        return Set.of(
                            ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE,
                            ResourceVisibility.ORGANIZATION);
                    }
                }));
    }
}

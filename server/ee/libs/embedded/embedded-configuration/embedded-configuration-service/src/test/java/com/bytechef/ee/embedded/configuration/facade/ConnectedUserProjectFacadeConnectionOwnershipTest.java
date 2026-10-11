/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * A connected user may bind and deploy only the connections they own: binding refuses a foreign connection, and
 * publishing drops any foreign connection left in the workflow's test configuration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserProjectFacadeConnectionOwnershipTest {

    private static final long CONNECTED_USER_ID = 7L;
    private static final String EXTERNAL_USER_ID = "alice";
    private static final long FOREIGN_CONNECTION_ID = 99L;
    private static final long OWNED_CONNECTION_ID = 11L;
    private static final long PROJECT_ID = 3L;
    private static final String WORKFLOW_ID = "workflow-id";
    private static final String WORKFLOW_UUID = "workflow-uuid";

    @Mock
    private ConnectedUserConnectionMembership connectedUserConnectionMembership;

    @Mock
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Mock
    private ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Mock
    private ProjectFacade projectFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private ConnectedUserProjectFacadeImpl connectedUserProjectFacade;

    @BeforeEach
    void setUp() {
        connectedUserProjectFacade = new ConnectedUserProjectFacadeImpl(
            null, null, connectedUserConnectionMembership, null, null, connectedUserProjectWorkflowManager, null,
            connectedUserProjectWorkflowService, null, null, null, connectionService, null, null, null, null, null,
            null, projectDeploymentFacade, null, null, projectFacade, null, null, projectWorkflowService, null, null,
            null, workflowTestConfigurationFacade, workflowTestConfigurationService);
    }

    @Test
    void testBindRefusesAConnectionTheConnectedUserDoesNotOwn() {
        givenConnectedUserWorkflow();

        assertThatThrownBy(
            () -> connectedUserProjectFacade.updateWorkflowConfigurationConnection(
                EXTERNAL_USER_ID, WORKFLOW_UUID, "slack_1", "slack", FOREIGN_CONNECTION_ID, Environment.PRODUCTION))
                    .isInstanceOf(AccessDeniedException.class);

        verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void testBindSavesAConnectionTheConnectedUserOwns() {
        givenConnectedUserWorkflow();

        connectedUserProjectFacade.updateWorkflowConfigurationConnection(
            EXTERNAL_USER_ID, WORKFLOW_UUID, "slack_1", "slack", OWNED_CONNECTION_ID, Environment.PRODUCTION);

        verify(workflowTestConfigurationFacade).saveWorkflowTestConfigurationConnection(
            WORKFLOW_ID, "slack_1", "slack", OWNED_CONNECTION_ID, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testPublishDeploysOnlyConnectionsTheConnectedUserOwns() {
        givenConnectedUserWorkflow();

        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getId()).thenReturn(5L);
        when(projectWorkflowService.getWorkflowProjectWorkflow(WORKFLOW_ID)).thenReturn(projectWorkflow);

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(
                new WorkflowTestConfigurationConnection(OWNED_CONNECTION_ID, "slack", "slack_1"),
                new WorkflowTestConfigurationConnection(FOREIGN_CONNECTION_ID, "slack", "slack_2")));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
            WORKFLOW_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.of(workflowTestConfiguration));

        Connection connection = new Connection();

        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());

        when(connectionService.getConnection(OWNED_CONNECTION_ID)).thenReturn(connection);
        when(projectFacade.publishProject(PROJECT_ID, "v2", false)).thenReturn(3);

        connectedUserProjectFacade.publishProjectWorkflow(EXTERNAL_USER_ID, WORKFLOW_UUID, "v2", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectDeploymentWorkflowConnection>> connectionsCaptor =
            ArgumentCaptor.forClass(List.class);

        verify(projectDeploymentFacade).updateProjectDeployment(
            eq(PROJECT_ID), eq(2), eq(WORKFLOW_UUID), connectionsCaptor.capture(), isNull());

        assertThat(connectionsCaptor.getValue())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(OWNED_CONNECTION_ID);

        verify(connectionService, never()).getConnection(FOREIGN_CONNECTION_ID);
    }

    private void givenConnectedUserWorkflow() {
        when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            EXTERNAL_USER_ID, Environment.PRODUCTION))
                .thenReturn(new ConnectedUserProject(CONNECTED_USER_ID, 1L, PROJECT_ID, 0));
        when(projectWorkflowService.fetchLastProjectWorkflowId(PROJECT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of(WORKFLOW_ID));
        when(connectedUserConnectionMembership.getOwnedConnectionIds(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(Set.of(OWNED_CONNECTION_ID));
    }
}
